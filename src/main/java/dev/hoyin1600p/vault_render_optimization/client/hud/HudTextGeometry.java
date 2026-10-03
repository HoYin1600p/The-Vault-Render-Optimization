package dev.hoyin1600p.vault_render_optimization.client.hud;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Matrix4f;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.FontSet;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;

/** No HUD values, item data, formatted sequences or game objects enter the key. */
public final class HudTextGeometry {
    private static final int MAX_ENTRIES = 128;
    private static final int MAX_BYTES = 2 * 1024 * 1024;
    private static final LinkedHashMap<Key, Entry> CACHE = new LinkedHashMap<>(16, 0.75F, true);
    private static Font cachedFont;
    private static FontSet cachedFontSet;
    private static final AtomicLong FONT_GENERATION = new AtomicLong();
    private static long cachedGeneration;
    private static boolean inHud;
    private static boolean capturing;
    private static int bytes;
    private static int width, height;
    private static double scale;
    private static long hits, misses, fallbacks;

    private HudTextGeometry() { }

    public static void begin(RenderGameOverlayEvent.Pre event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) return;
        inHud = false;
        if (event.isCanceled()) return;
        if (!ClientOptimizationConfig.optimizationsEnabled() || !ClientOptimizationConfig.hudTextGeometry) {
            clear();
            return;
        }
        var window = event.getWindow();
        if (width != window.getWidth() || height != window.getHeight() || scale != window.getGuiScale()) clear();
        width = window.getWidth(); height = window.getHeight(); scale = window.getGuiScale();
        inHud = true;
    }

    public static void end(RenderGameOverlayEvent.Post event) {
        if (event.getType() == RenderGameOverlayEvent.ElementType.ALL) inHud = false;
    }

    public static void frameBoundary(TickEvent.RenderTickEvent event) {
        // Also restores scope if a canceled overlay or an exception skipped Post ALL.
        inHud = false;
    }

    public static void clear() {
        CACHE.clear(); bytes = 0; cachedFont = null; cachedFontSet = null; inHud = false;
    }

    public static void invalidateFonts() { FONT_GENERATION.incrementAndGet(); }

    public static boolean eligibleScope() {
        return inHud && !capturing && ClientOptimizationConfig.hudTextGeometry
                && ClientOptimizationConfig.optimizationsEnabled() && RenderSystem.isOnRenderThread();
    }

    public static Integer draw(Font font, FontSet fontSet, String text, float x, float y, int color, boolean shadow,
                               Matrix4f pose, MultiBufferSource buffers, boolean seeThrough,
                               int background, int light, boolean bidi) {
        if (!eligibleScope() || font.getClass() != Font.class || fontSet == null || fontSet.getClass() != FontSet.class
                || buffers.getClass() != MultiBufferSource.BufferSource.class
                || bidi || !eligible(text) || !Float.isFinite(x) || !Float.isFinite(y)) return null;
        long generation = FONT_GENERATION.get();
        if (cachedFont != font || cachedFontSet != fontSet || cachedGeneration != generation) {
            CACHE.clear(); bytes = 0; cachedFont = font; cachedFontSet = fontSet; cachedGeneration = generation;
        }
        Key lookup = new Key(text, x, y, color, shadow, seeThrough, background, light, pose);
        Entry entry = CACHE.get(lookup);
        if (entry == null) {
            misses++;
            HudGlyphMesh mesh = new HudGlyphMesh();
            int result;
            capturing = true;
            try {
                // Capture exact native matrix arithmetic, not a reconstructed shadow offset.
                result = font.drawInBatch(text, x, y, color, shadow, pose.copy(), mesh, seeThrough, background, light, false);
                mesh.freeze();
            } catch (UnsupportedOperationException unsupported) {
                mesh = null; result = 0;
            } finally {
                capturing = false;
            }
            entry = new Entry(mesh, result);
            int size = mesh == null ? 0 : mesh.bytes();
            while (!CACHE.isEmpty() && (CACHE.size() >= MAX_ENTRIES || bytes + size > MAX_BYTES)) {
                var iterator = CACHE.entrySet().iterator();
                Entry evicted = iterator.next().getValue();
                if (evicted.mesh != null) bytes -= evicted.mesh.bytes();
                iterator.remove();
            }
            CACHE.put(new Key(text, x, y, color, shadow, seeThrough, background, light, pose.copy()), entry);
            bytes += size;
        } else hits++;
        if (entry.mesh == null) { fallbacks++; return null; }
        // Existing callers still flush, set shader state, clip and draw at the native cadence.
        entry.mesh.render(buffers);
        return entry.result;
    }

    public static String status() {
        return "entries=" + CACHE.size() + ", vertex bytes=" + bytes + ", hits=" + hits
                + ", misses=" + misses + ", unsupported-output fallbacks=" + fallbacks;
    }

    public static boolean eligible(String text) {
        if (text == null || text.isEmpty() || text.length() > 64) return false;
        for (int i = 0; i < text.length(); i++) if (text.charAt(i) < 32 || text.charAt(i) > 126) return false;
        return true;
    }

    record Key(String text, float x, float y, int color, boolean shadow, boolean seeThrough,
                       int background, int light, Matrix4f pose) { }
    private record Entry(HudGlyphMesh mesh, int result) { }
}
