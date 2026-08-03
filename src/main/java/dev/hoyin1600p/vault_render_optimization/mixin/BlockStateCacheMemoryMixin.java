package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.memory.FaceSturdyArrayInterner;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class BlockStateCacheMemoryMixin {
    @Shadow
    @Final
    @Mutable
    private boolean[] faceSturdy;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void vault_render_optimization$deduplicateFaceSturdy(
            BlockState state, CallbackInfo callbackInfo) {
        this.faceSturdy = FaceSturdyArrayInterner.intern(this.faceSturdy);
    }
}
