package dev.hoyin1600p.vault_render_optimization.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "iskallia.vaultintegrations.block.entity.AltarConduitBlockEntity", remap = false)
public abstract class AltarConduitClientCrashGuardMixin {
    @Shadow
    private BlockPos placedPos;

    @Inject(method = "tick", at = @At("HEAD"))
    private static void vault_render_optimization$initializePlacedPosition(
            Level level,
            BlockPos pos,
            BlockState state,
            @Coerce Object altarConduitObject,
            CallbackInfo ci
    ) {
        AltarConduitClientCrashGuardMixin altarConduit =
                (AltarConduitClientCrashGuardMixin) altarConduitObject;

        if (altarConduit.placedPos == null) {
            altarConduit.placedPos = pos.immutable();
        }
    }
}
