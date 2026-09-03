package dev.hoyin1600p.vault_render_optimization.mixin.chunk;

import dev.hoyin1600p.vault_render_optimization.client.chunk.ChunkUpdateState;
import net.minecraft.client.Options;
import net.minecraft.client.PrioritizeChunkUpdates;
import net.minecraft.client.renderer.LevelRenderer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Selects vanilla's normal asynchronous compile list, preserving dirty flags and upload handling. */
@Mixin(LevelRenderer.class)
public abstract class VanillaDeferredChunkUpdatesMixin {
    @Redirect(
            method = "compileChunks",
            at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/Options;prioritizeChunkUpdates:Lnet/minecraft/client/PrioritizeChunkUpdates;"),
            require = 2, allow = 2
    )
    private PrioritizeChunkUpdates vro$selectNativeAsyncCompileList(Options options) {
        PrioritizeChunkUpdates original = options.prioritizeChunkUpdates;
        return ChunkUpdateState.defer(original == PrioritizeChunkUpdates.NONE)
                ? PrioritizeChunkUpdates.NONE : original;
    }
}
