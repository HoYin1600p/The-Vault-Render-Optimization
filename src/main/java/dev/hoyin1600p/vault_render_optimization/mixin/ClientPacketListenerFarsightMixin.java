package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.chunk.residency.FarsightChunkBound;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.network.protocol.game.ClientboundLoginPacket;
import net.minecraft.network.protocol.game.ClientboundSetChunkCacheRadiusPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds {@link FarsightChunkBound}; only applied when Farsight is installed. The HEAD hooks read
 * the radius straight from the packet (Farsight redirects only the reads inside the handler
 * bodies) and run before {@code ensureRunningOnSameThread}, so they check the thread themselves.
 */
@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerFarsightMixin {
    @Inject(method = "handleLogin", at = @At("HEAD"))
    private void vro$recordLoginChunkRadius(ClientboundLoginPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            FarsightChunkBound.recordServerChunkRadius(packet.chunkRadius());
        }
    }

    @Inject(method = "handleSetChunkCacheRadius", at = @At("HEAD"))
    private void vro$recordServerChunkRadius(ClientboundSetChunkCacheRadiusPacket packet, CallbackInfo ci) {
        if (Minecraft.getInstance().isSameThread()) {
            FarsightChunkBound.recordServerChunkRadius(packet.getRadius());
        }
    }

    @Inject(method = "handleLevelChunkWithLight", at = @At("RETURN"))
    private void vro$trackLoadedChunk(ClientboundLevelChunkWithLightPacket packet, CallbackInfo ci) {
        FarsightChunkBound.onChunkLoaded(((ClientPacketListener) (Object) this).getLevel(), packet.getX(), packet.getZ());
    }
}
