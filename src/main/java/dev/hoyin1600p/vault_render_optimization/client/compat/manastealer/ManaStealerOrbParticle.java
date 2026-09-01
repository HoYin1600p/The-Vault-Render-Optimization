package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleBillboardGeometry;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

final class ManaStealerOrbParticle extends TextureSheetParticle {
    private static final float OUTER_RED = 0.84F;
    private static final float OUTER_GREEN = 0.96F;
    private static final float OUTER_BLUE = 1.0F;
    private static final float OUTER_ALPHA = 0.78F;
    private static final float INNER_RED = 0.035F;
    private static final float INNER_GREEN = 0.12F;
    private static final float INNER_BLUE = 0.34F;
    private static final float INNER_ALPHA = 0.94F;

    private static volatile SpriteSet sprites;

    private final int sourceEntityId;
    private final double startX;
    private final double startY;
    private final double startZ;
    private final double centerX;
    private final double centerY;
    private final double centerZ;
    private final float initialDiameter;
    private final float innerRatio;
    private long lastParticleTick;
    private boolean removalReported;

    ManaStealerOrbParticle(
            ClientLevel level,
            int sourceEntityId,
            double startX,
            double startY,
            double startZ,
            double centerX,
            double centerY,
            double centerZ,
            int lifetime,
            float initialDiameter,
            float innerRatio
    ) {
        super(level, startX, startY, startZ);
        this.sourceEntityId = sourceEntityId;
        this.startX = startX;
        this.startY = startY;
        this.startZ = startZ;
        this.centerX = centerX;
        this.centerY = centerY;
        this.centerZ = centerZ;
        this.lifetime = Math.max(1, lifetime);
        this.initialDiameter = initialDiameter;
        this.innerRatio = innerRatio;
        this.lastParticleTick = level.getGameTime();
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.xd = 0.0D;
        this.yd = 0.0D;
        this.zd = 0.0D;
        this.pickSprite(sprites);
        this.updateBounds(initialDiameter);
    }

    static void bindSprites(SpriteSet value) {
        sprites = value;
    }

    static boolean spritesReady() {
        return sprites != null;
    }

    boolean tickedRecently(long gameTime) {
        return this.lastParticleTick >= gameTime - 2L;
    }

    @Override
    public void tick() {
        this.lastParticleTick = this.level.getGameTime();
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        this.age++;
        if (this.age >= this.lifetime) {
            this.setPos(this.centerX, this.centerY, this.centerZ);
            this.remove();
            return;
        }

        double progress = (double) this.age / (double) this.lifetime;
        this.setPos(
                Mth.lerp(progress, this.startX, this.centerX),
                Mth.lerp(progress, this.startY, this.centerY),
                Mth.lerp(progress, this.startZ, this.centerZ)
        );
        this.updateBounds(this.initialDiameter * ManaStealerOrbKinematics.scale((float) progress));
    }

    @Override
    public void render(VertexConsumer consumer, Camera camera, float partialTick) {
        Vec3 cameraPosition = camera.getPosition();
        float positionX = (float) (Mth.lerp(partialTick, this.xo, this.x) - cameraPosition.x());
        float positionY = (float) (Mth.lerp(partialTick, this.yo, this.y) - cameraPosition.y());
        float positionZ = (float) (Mth.lerp(partialTick, this.zo, this.z) - cameraPosition.z());
        Vector3f left = camera.getLeftVector();
        Vector3f up = camera.getUpVector();
        float progress = Math.min(1.0F, (this.age + partialTick) / (float) this.lifetime);
        float outerRadius = this.initialDiameter * 0.5F * ManaStealerOrbKinematics.scale(progress);
        int light = this.getLightColor(partialTick);

        this.writeLayer(
                consumer, left, up, outerRadius, positionX, positionY, positionZ,
                OUTER_RED, OUTER_GREEN, OUTER_BLUE, OUTER_ALPHA, light
        );
        this.writeLayer(
                consumer, left, up, outerRadius * this.innerRatio, positionX, positionY, positionZ,
                INNER_RED, INNER_GREEN, INNER_BLUE, INNER_ALPHA, light
        );
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public boolean shouldCull() {
        return true;
    }

    @Override
    public void remove() {
        super.remove();
        if (!this.removalReported) {
            this.removalReported = true;
            ManaStealerVisualController.onParticleRemoved(this.sourceEntityId, this);
        }
    }

    private void writeLayer(
            VertexConsumer consumer,
            Vector3f left,
            Vector3f up,
            float radius,
            float positionX,
            float positionY,
            float positionZ,
            float red,
            float green,
            float blue,
            float alpha,
            int light
    ) {
        ParticleBillboardGeometry geometry = ParticleBillboardGeometry.compute(
                left.x(), left.y(), left.z(),
                up.x(), up.y(), up.z(),
                0.0F,
                radius,
                positionX, positionY, positionZ
        );
        float minU = this.getU0();
        float maxU = this.getU1();
        float minV = this.getV0();
        float maxV = this.getV1();

        this.write(consumer, geometry.x0(), geometry.y0(), geometry.z0(), maxU, maxV,
                red, green, blue, alpha, light);
        this.write(consumer, geometry.x1(), geometry.y1(), geometry.z1(), maxU, minV,
                red, green, blue, alpha, light);
        this.write(consumer, geometry.x2(), geometry.y2(), geometry.z2(), minU, minV,
                red, green, blue, alpha, light);
        this.write(consumer, geometry.x3(), geometry.y3(), geometry.z3(), minU, maxV,
                red, green, blue, alpha, light);
    }

    private void write(
            VertexConsumer consumer,
            float x,
            float y,
            float z,
            float u,
            float v,
            float red,
            float green,
            float blue,
            float alpha,
            int light
    ) {
        consumer.vertex(x, y, z)
                .uv(u, v)
                .color(red, green, blue, alpha)
                .uv2(light)
                .endVertex();
    }

    private void updateBounds(float diameter) {
        double half = Math.max(0.01D, diameter * 0.5D);
        this.setBoundingBox(new AABB(
                this.x - half, this.y - half, this.z - half,
                this.x + half, this.y + half, this.z + half
        ));
    }
}
