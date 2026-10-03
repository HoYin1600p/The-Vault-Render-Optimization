package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.chunk.residency.FarsightChunkBound;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Records which chunks the server has forgotten for {@link FarsightChunkBound}; only applied when
 * Farsight is installed. Hooked on the packet rather than on
 * {@code ClientPacketListener.handleForgetLevelChunk}: Farsight cancels that handler at HEAD, and a
 * second HEAD injection there would depend on mixin ordering. The packet's own {@code handle} always
 * runs, on the network thread when Farsight cancels before the handler reschedules itself.
 */
@Mixin(ClientboundForgetLevelChunkPacket.class)
public abstract class ForgetLevelChunkPacketFarsightMixin {
    @Inject(method = "handle(Lnet/minecraft/network/protocol/game/ClientGamePacketListener;)V", at = @At("HEAD"))
    private void vro$recordServerForget(ClientGamePacketListener listener, CallbackInfo ci) {
        ClientboundForgetLevelChunkPacket packet = (ClientboundForgetLevelChunkPacket) (Object) this;
        FarsightChunkBound.onServerForget(packet.getX(), packet.getZ());
    }
}
