package dev.hoyin1600p.vault_render_optimization.mixin;

import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = RenderSection.class, remap = false)
public interface SodiumRenderSectionAccessor {
    @Invoker("getChunkX")
    int vault_render_optimization$getChunkX();

    @Invoker("getChunkY")
    int vault_render_optimization$getChunkY();

    @Invoker("getChunkZ")
    int vault_render_optimization$getChunkZ();
}
