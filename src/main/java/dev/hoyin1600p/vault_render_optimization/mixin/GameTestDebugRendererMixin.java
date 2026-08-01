package dev.hoyin1600p.vault_render_optimization.mixin;

import dev.hoyin1600p.vault_render_optimization.client.render.GameTestMarkerState;
import net.minecraft.client.renderer.debug.GameTestDebugRenderer;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(GameTestDebugRenderer.class)
public abstract class GameTestDebugRendererMixin implements GameTestMarkerState {
    @Shadow
    @Final
    private Map<BlockPos, ?> markers;

    @Override
    public boolean vro$hasGameTestMarkers() {
        return !this.markers.isEmpty();
    }
}
