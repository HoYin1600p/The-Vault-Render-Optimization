package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.VaultMapKeyConflictContext;
import iskallia.vault.init.ModKeybinds;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ModKeybinds.class, remap = false)
public abstract class VaultMapKeybindMixin {
    @Inject(method = "register", at = @At("TAIL"))
    private static void vaultRenderOptimization$limitOpenMapKeyToVaults(
            FMLClientSetupEvent event, CallbackInfo callbackInfo) {
        ModKeybinds.openVaultMap.setKeyConflictContext(VaultMapKeyConflictContext.INSTANCE);
    }
}
