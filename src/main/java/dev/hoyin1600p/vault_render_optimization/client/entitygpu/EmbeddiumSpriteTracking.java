package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * Embeddium and Rubidium only advance an animated texture that was marked as drawn this frame ("Animate Only
 * Visible Textures"). Their {@code renderQuadList} overwrite marks each quad's sprite; when VRO draws a list on
 * the GPU that overwrite does not run, so VRO marks the same sprites itself. Loaded only while the Embeddium
 * writer is active, which means one of the two is installed.
 */
final class EmbeddiumSpriteTracking {
    private static boolean available = true;

    private EmbeddiumSpriteTracking() {
    }

    static void markActive(TextureAtlasSprite[] sprites) {
        if (!available) return;
        try {
            for (TextureAtlasSprite sprite : sprites) {
                Embeddium.markActive(sprite);
            }
        } catch (LinkageError failure) {
            available = false;
            VaultRenderOptimization.LOGGER.warn("Embeddium's sprite tracking is unavailable; GPU items will not mark "
                    + "animated textures as drawn", failure);
        }
    }

    /** Loaded only when the renderer is installed. */
    private static final class Embeddium {
        static void markActive(TextureAtlasSprite sprite) {
            me.jellysquid.mods.sodium.client.render.texture.SpriteUtil.markSpriteActive(sprite);
        }
    }
}
