package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Vector3f;
import net.minecraft.util.Mth;

/**
 * Billboard particles on the GPU. VRO's billboard writers call {@link #tryReserve} with exactly the inputs they
 * would give {@code ParticleBillboardGeometry.compute}; when the consumer is a plain {@code PARTICLE}
 * {@code BufferBuilder} and the GPU path is active, the four vertices are reserved and the compute shader
 * writes them with the same arithmetic. Otherwise the writer continues on the CPU. No particle is skipped.
 */
public final class GpuParticles {
    private GpuParticles() {
    }

    /** @return true when the particle's vertices were reserved and the caller must not write them */
    public static boolean tryReserve(VertexConsumer consumer, float x, float y, float z, Vector3f left, Vector3f up,
                                     float angle, float size, float minU, float maxU, float minV, float maxV, int color,
                                     int light) {
        if (consumer.getClass() != BufferBuilder.class || !GpuEntityModels.particleFrameActive()
                || !RenderSystem.isOnRenderThread()) {
            return false;
        }
        GpuHoleBuilder builder = (GpuHoleBuilder) consumer;
        if (!builder.vro$canReserveParticle()) return false;
        boolean rolled = angle != 0.0F;
        return builder.vro$reserveParticle(x, y, z, size, rolled, rolled ? Mth.sin(angle) : 0.0F,
                rolled ? Mth.cos(angle) : 1.0F, minU, maxU, minV, maxV, color, light,
                left.x(), left.y(), left.z(), up.x(), up.y(), up.z());
    }
}
