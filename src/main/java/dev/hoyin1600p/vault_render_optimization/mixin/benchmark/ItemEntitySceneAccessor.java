package dev.hoyin1600p.vault_render_optimization.mixin.benchmark;

import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ItemEntity.class)
public interface ItemEntitySceneAccessor {
    @Accessor("age")
    void vro$age(int age);

    @Mutable
    @Accessor("bobOffs")
    void vro$bobOffset(float offset);
}
