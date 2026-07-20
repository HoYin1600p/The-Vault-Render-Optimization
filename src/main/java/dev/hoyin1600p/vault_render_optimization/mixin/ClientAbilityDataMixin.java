package dev.hoyin1600p.vault_render_optimization.mixin;

import iskallia.vault.client.data.ClientAbilityData;
import iskallia.vault.network.message.AbilityKnownOnesMessage;
import iskallia.vault.skill.base.TieredSkill;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ClientAbilityData.class, remap = false)
public abstract class ClientAbilityDataMixin {
    @Unique
    private static List<TieredSkill> vault_render_optimization$learnedAbilities;

    @Inject(method = "getLearnedAbilities", at = @At("HEAD"), cancellable = true)
    private static void vault_render_optimization$reuseLearnedAbilities(CallbackInfoReturnable<List<TieredSkill>> cir) {
        List<TieredSkill> cached = vault_render_optimization$learnedAbilities;
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getLearnedAbilities", at = @At("RETURN"))
    private static void vault_render_optimization$rememberLearnedAbilities(CallbackInfoReturnable<List<TieredSkill>> cir) {
        if (vault_render_optimization$learnedAbilities == null) {
            vault_render_optimization$learnedAbilities = cir.getReturnValue();
        }
    }

    @Inject(method = "updateAbilities", at = @At("TAIL"))
    private static void vault_render_optimization$invalidateLearnedAbilities(
            AbilityKnownOnesMessage message,
            CallbackInfo ci
    ) {
        vault_render_optimization$learnedAbilities = null;
    }
}
