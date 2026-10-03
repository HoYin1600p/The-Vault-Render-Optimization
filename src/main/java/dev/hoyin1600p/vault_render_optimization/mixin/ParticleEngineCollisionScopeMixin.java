package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.particle.ParticleCollisionCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Bounds the collision cell cache to one particle tick, when no block can change. */
@Mixin(ParticleEngine.class)
public abstract class ParticleEngineCollisionScopeMixin {
    @Shadow
    protected ClientLevel level;

    @Inject(method = "tick", at = @At("HEAD"))
    private void vro$beginCollisionScope(CallbackInfo ci) {
        ParticleCollisionCache.begin(this.level);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void vro$endCollisionScope(CallbackInfo ci) {
        ParticleCollisionCache.end();
    }

    @Inject(method = "setLevel", at = @At("HEAD"))
    private void vro$dropCollisionScope(CallbackInfo ci) {
        ParticleCollisionCache.end();
    }
}
