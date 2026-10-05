package dev.hoyin1600p.vault_render_optimization.mixin.benchmark;

import net.minecraft.world.entity.decoration.ArmorStand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ArmorStand.class)
public interface ArmorStandSceneAccessor {
    @Invoker("setShowArms")
    void vro$showArms(boolean showArms);
}
