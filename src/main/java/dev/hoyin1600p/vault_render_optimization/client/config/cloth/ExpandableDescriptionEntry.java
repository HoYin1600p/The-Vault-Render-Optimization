package dev.hoyin1600p.vault_render_optimization.client.config.cloth;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.hoyin1600p.vault_render_optimization.util.VroGuiColors;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

/**
 * The grey "▶ summary" row under a setting. Clicking anywhere on it shows the full explanation
 * underneath ("▼ summary"); clicking again hides it. Text wraps to the width Cloth gives the row
 * (at most {@link #MAX_WIDTH}), and the row's height follows its current line count, which Cloth's
 * list re-reads every frame, so resizing and expanding reflow and scroll correctly.
 */
final class ExpandableDescriptionEntry extends AbstractConfigListEntry<Object> {
    private static final int MAX_WIDTH = 380;
    private static final int LINE_HEIGHT = 10;
    private static final int PADDING = 2;
    private static final int INDENT = 8;

    private final Component summary;
    private final Component description;
    private boolean expanded;
    private int laidOutWidth = -1;
    private boolean laidOutExpanded;
    private List<FormattedCharSequence> summaryLines = List.of();
    private List<FormattedCharSequence> descriptionLines = List.of();

    private final Component owner;

    /**
     * @param owner the label of the setting this row sits under; Cloth's search matches it as well as
     *              the summary, so a row stays visible whenever its setting is
     */
    ExpandableDescriptionEntry(Component owner, Component summary, Component description) {
        super(summary, false);
        this.owner = owner;
        this.summary = summary;
        this.description = description;
    }

    /**
     * Cloth filters entries by these tags (case-insensitive contains). The default is only the field
     * name (the summary), which hid the row when a search matched just the setting's label.
     */
    @Override
    public Iterator<String> getSearchTags() {
        String ownerText = owner.getString();
        String summaryText = summary.getString();
        return List.of(ownerText, summaryText, ownerText + " " + summaryText).iterator();
    }

    private void layout(int width) {
        if (width == laidOutWidth && expanded == laidOutExpanded) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        Component row = new TextComponent(expanded ? "▼ " : "▶ ").append(summary.copy());
        summaryLines = font.split(row, Math.max(20, width));
        descriptionLines = expanded ? font.split(description, Math.max(20, width - INDENT)) : List.of();
        laidOutWidth = width;
        laidOutExpanded = expanded;
    }

    @Override
    public void render(PoseStack poseStack, int index, int y, int x, int entryWidth, int entryHeight,
                       int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(poseStack, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        layout(Math.min(entryWidth, MAX_WIDTH));
        Font font = Minecraft.getInstance().font;
        int lineY = y + PADDING / 2;
        int summaryColor = hovered ? VroGuiColors.SUMMARY_HOVER : VroGuiColors.SUMMARY;
        for (FormattedCharSequence line : summaryLines) {
            font.drawShadow(poseStack, line, x, lineY, summaryColor);
            lineY += LINE_HEIGHT;
        }
        for (FormattedCharSequence line : descriptionLines) {
            font.drawShadow(poseStack, line, x + INDENT, lineY, VroGuiColors.DESCRIPTION);
            lineY += LINE_HEIGHT;
        }
    }

    @Override
    public int getItemHeight() {
        // Before the first render there is no width yet: one summary line.
        int lines = laidOutWidth < 0 ? 1 : summaryLines.size() + descriptionLines.size();
        return lines * LINE_HEIGHT + PADDING;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0 || !isMouseOver(mouseX, mouseY)) {
            return false;
        }
        expanded = !expanded;
        if (laidOutWidth >= 0) {
            // Re-lay out now so the list sees the new height before the next frame.
            int width = laidOutWidth;
            laidOutWidth = -2;
            layout(width);
        }
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
        return true;
    }

    @Override
    public Object getValue() {
        return null;
    }

    @Override
    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public List<? extends GuiEventListener> children() {
        return new ArrayList<>();
    }

    @Override
    public List<? extends NarratableEntry> narratables() {
        return new ArrayList<>();
    }
}
