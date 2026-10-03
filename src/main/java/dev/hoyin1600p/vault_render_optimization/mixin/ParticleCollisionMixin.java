package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCollisionCache;
import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCollisionState;
import java.util.List;
import net.minecraft.client.particle.Particle;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Particle.class)
public abstract class ParticleCollisionMixin {
    @Redirect(method = "move", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/entity/Entity;collideBoundingBox(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/AABB;Lnet/minecraft/world/level/Level;Ljava/util/List;)Lnet/minecraft/world/phys/Vec3;"), require = 1)
    private Vec3 vro$collideCached(Entity entity, Vec3 motion, AABB box, Level level, List<VoxelShape> collisions) {
        // Particle.move always passes a null entity and no extra shapes, so no world border applies.
        if (entity != null || !collisions.isEmpty() || !ParticleCollisionState.enabled()
                || !ParticleCollisionCache.active(level)) {
            ParticleCollisionState.recordVanilla();
            return Entity.collideBoundingBox(entity, motion, box, level, collisions);
        }
        List<VoxelShape> shapes = ParticleCollisionCache.shapes(box.expandTowards(motion));
        Vec3 cached = shapes.isEmpty() ? motion : EntityCollisionInvoker.vro$collideWithShapes(motion, box, shapes);
        ParticleCollisionState.recordCached(shapes.isEmpty());
        if (ParticleCollisionState.verifying()) {
            Vec3 vanilla = Entity.collideBoundingBox(null, motion, box, level, collisions);
            ParticleCollisionState.recordVerification(vanilla.equals(cached), box, motion, vanilla, cached);
            // Verification returns vanilla's result, so a mismatch is only counted, never shown.
            return vanilla;
        }
        return cached;
    }
}
