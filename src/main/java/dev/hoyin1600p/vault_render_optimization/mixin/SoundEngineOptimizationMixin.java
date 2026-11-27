package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import dev.hoyin1600p.vault_render_optimization.client.sound.SoundOptimization;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SoundEngine.class)
public abstract class SoundEngineOptimizationMixin {
    @Shadow
    @Final
    private Library library;

    @Shadow
    @Final
    private Listener listener;

    @Inject(
            method = "play",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/resources/sounds/SoundInstance;getVolume()F"
            ),
            cancellable = true
    )
    private void vro$cullInaudibleOrSaturatedSound(SoundInstance soundInstance, CallbackInfo ci) {
        Sound sound = soundInstance.getSound();
        if (sound == null || sound == SoundManager.EMPTY_SOUND) {
            return;
        }
        Library.ChannelPool staticPool = ((LibraryChannelPoolAccessor) this.library).vro$getStaticChannels();
        if (staticPool != null && SoundOptimization.shouldCullSaturatedStaticChannel(
                sound.shouldStream(),
                staticPool.getUsedCount(),
                staticPool.getMaxCount()
        )) {
            ci.cancel();
            return;
        }
        double sx = soundInstance.getX();
        double sy = soundInstance.getY();
        double sz = soundInstance.getZ();
        Vec3 listenerPos = this.listener.getListenerPosition();
        double minDistSq = SoundOptimization.distanceSquared(
                listenerPos.x, listenerPos.y, listenerPos.z, sx, sy, sz);
        // Listener and Camera positions only update during renderLevel; after a teleport
        // (e.g. Elevator blocks, Waystones, Ender Pearls) the player has already moved on the
        // client tick before the next frame updates the OpenAL listener. Keep any sound within
        // range of either the listener, main camera, or current player eye position.
        Minecraft mc = Minecraft.getInstance();
        if (mc != null) {
            if (mc.gameRenderer != null) {
                Camera camera = mc.gameRenderer.getMainCamera();
                if (camera != null) {
                    Vec3 camPos = camera.getPosition();
                    double camDistSq = SoundOptimization.distanceSquared(
                            camPos.x, camPos.y, camPos.z, sx, sy, sz);
                    if (camDistSq < minDistSq) {
                        minDistSq = camDistSq;
                    }
                }
            }
            LocalPlayer player = mc.player;
            if (player != null) {
                Vec3 eyePos = player.getEyePosition();
                double playerDistSq = SoundOptimization.distanceSquared(
                        eyePos.x, eyePos.y, eyePos.z, sx, sy, sz);
                if (playerDistSq < minDistSq) {
                    minDistSq = playerDistSq;
                }
            }
        }
        if (SoundOptimization.shouldCullOutOfRange(
                soundInstance.isRelative(),
                soundInstance.isLooping(),
                soundInstance instanceof TickableSoundInstance,
                soundInstance.getAttenuation() == SoundInstance.Attenuation.LINEAR,
                soundInstance.canStartSilent(),
                soundInstance.getVolume(),
                sound.getAttenuationDistance(),
                minDistSq
        )) {
            ci.cancel();
        }
    }
}
