package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import com.mojang.math.Matrix4f;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.util.FormattedCharSequence;
import net.p3pp3rf1y.sophisticatedcore.util.CountAbbreviator;
import net.p3pp3rf1y.sophisticatedstorage.block.LimitedBarrelBlockEntity;
import net.p3pp3rf1y.sophisticatedstorage.block.VerticalFacing;
import net.p3pp3rf1y.sophisticatedstorage.client.render.DisplayItemRenderer;

import static net.minecraft.client.Minecraft.UNIFORM_FONT;

/**
 * Allocation-bounded adaptation of Sophisticated Storage's 1.18.2 limited
 * barrel count layout. Upstream revision 891b0d9e29350ec78c7b8567285036d2ecdb303c,
 * GPL-3.0-only; cache policy and lifecycle are VRO changes.
 */
public final class LimitedBarrelCountRenderer {
    private static final float MULTIPLE_ITEMS_FONT_SCALE = 1.0F / 96.0F;
    private static final float SINGLE_ITEM_FONT_SCALE = 1.0F / 48.0F;
    private static final Style COUNT_DISPLAY_STYLE = Style.EMPTY.withFont(UNIFORM_FONT).withBold(true);
    private static final int MAX_LABELS = 4096;
    private static final Long2ObjectLinkedOpenHashMap<CachedLabel> LABELS =
            new Long2ObjectLinkedOpenHashMap<>();
    private static Font cachedFont;

    private LimitedBarrelCountRenderer() {
    }

    public static void render(
            LimitedBarrelBlockEntity barrel,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            boolean flatTop,
            Direction horizontalFacing,
            VerticalFacing verticalFacing
    ) {
        if (!barrel.shouldShowCounts()) {
            return;
        }

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);
        poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(horizontalFacing.getOpposite()));
        if (verticalFacing != VerticalFacing.NO) {
            poseStack.mulPose(DisplayItemRenderer.getNorthBasedRotation(verticalFacing.getDirection().getOpposite()));
        }
        poseStack.translate(0.5D, -0.5D, 0.5D);

        List<Integer> counts = barrel.getSlotCounts();
        int slots = counts.size();
        float yOffset = -(slots == 1 ? 0.25F : 0.11F);
        float scale = slots == 1 ? SINGLE_ITEM_FONT_SCALE : MULTIPLE_ITEMS_FONT_SCALE;
        int maxCharacters = slots == 1 ? 6 : 5;
        Font font = Minecraft.getInstance().font;

        for (int slot = 0; slot < slots; slot++) {
            int count = counts.get(slot);
            if (count <= 0) {
                continue;
            }

            CachedLabel label = label(font, count, maxCharacters);
            Vector3f frontOffset = DisplayItemRenderer.getDisplayItemIndexFrontOffset(slot, slots);
            poseStack.pushPose();
            poseStack.translate(
                    -frontOffset.x(),
                    frontOffset.y() + yOffset,
                    0.001D - (flatTop ? 0.0D : 0.75D / 16.0D)
            );
            poseStack.scale(scale, -scale, scale);
            poseStack.translate(-label.width() / 2.0F, 0.0D, 0.0D);
            if (label.mesh() != null) {
                label.mesh().render(poseStack.last().pose(), bufferSource, barrel.getSlotColor(slot), packedLight);
            } else font.drawInBatch(
                    label.glyphs(),
                    0.0F,
                    0.0F,
                    barrel.getSlotColor(slot),
                    false,
                    poseStack.last().pose(),
                    bufferSource,
                    false,
                    0,
                    packedLight
            );
            poseStack.popPose();
            SophisticatedStorageDiagnostics.recordCountLabelRendered();
        }
        poseStack.popPose();
    }

    public static void clear() {
        LABELS.clear();
        cachedFont = null;
    }

    static long cacheKey(int count, int maxCharacters) {
        return ((long) maxCharacters << 32) ^ (count & 0xFFFF_FFFFL);
    }

    private static CachedLabel label(Font font, int count, int maxCharacters) {
        if (cachedFont != font) {
            clear();
            cachedFont = font;
        }
        long key = cacheKey(count, maxCharacters);
        CachedLabel cached = LABELS.getAndMoveToFirst(key);
        if (cached != null) {
            SophisticatedStorageDiagnostics.recordCountCacheHit();
            return cached;
        }

        FormattedCharSequence glyphs = new TextComponent(
                CountAbbreviator.abbreviate(count, maxCharacters)
        ).withStyle(COUNT_DISPLAY_STYLE).getVisualOrderText();
        CountGlyphMesh mesh = new CountGlyphMesh();
        try {
            Matrix4f identity = new Matrix4f();
            identity.setIdentity();
            font.drawInBatch(glyphs, 0, 0, -1, false, identity, mesh, false, 0, 0);
            mesh.freeze();
        } catch (UnsupportedOperationException unsupported) {
            // Unknown font vertex output: no real buffers were touched; keep vanilla rendering.
            mesh = null;
        }
        CachedLabel created = new CachedLabel(glyphs, font.width(glyphs), mesh);
        if (LABELS.size() >= MAX_LABELS) {
            LABELS.removeLast();
        }
        LABELS.putAndMoveToFirst(key, created);
        SophisticatedStorageDiagnostics.recordCountCacheMiss();
        return created;
    }

    private record CachedLabel(FormattedCharSequence glyphs, float width, CountGlyphMesh mesh) {
    }
}
