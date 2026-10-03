package dev.hoyin1600p.vault_render_optimization.mixin;

import java.util.List;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Vanilla's own axis-ordered response, so cached collision keeps vanilla motion results exactly. */
@Mixin(Entity.class)
public interface EntityCollisionInvoker {
    @Invoker("collideWithShapes")
    static Vec3 vro$collideWithShapes(Vec3 motion, AABB box, List<VoxelShape> shapes) {
        throw new AssertionError();
    }
}
