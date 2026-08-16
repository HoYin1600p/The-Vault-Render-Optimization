package dev.hoyin1600p.vault_render_optimization.client.create;

import com.jozufozu.flywheel.backend.Backend;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import com.simibubi.create.content.contraptions.Contraption;
import com.simibubi.create.content.contraptions.ContraptionHandler;
import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;

import java.lang.ref.WeakReference;

public final class CreateDiagnostics {
    private CreateDiagnostics() {
    }

    public static int report(CommandSourceStack source) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            source.sendSuccess(new TextComponent("[VRO] Create diagnostics require a loaded world."), false);
            return 0;
        }

        int contraptions = 0;
        int blocks = 0;
        int renderedBlocks = 0;
        int specialBlockEntities = 0;
        int actors = 0;
        int largest = 0;

        for (WeakReference<AbstractContraptionEntity> reference
                : ContraptionHandler.loadedContraptions.get(minecraft.level).values()) {
            AbstractContraptionEntity entity = reference.get();
            if (entity == null || entity.getContraption() == null) {
                continue;
            }
            Contraption contraption = entity.getContraption();
            int blockCount = contraption.getBlocks().size();
            contraptions++;
            blocks += blockCount;
            renderedBlocks += contraption.getRenderedBlocks().size();
            specialBlockEntities += contraption.getSpecialRenderedBEs().size();
            actors += contraption.getActors().size();
            largest = Math.max(largest, blockCount);
        }

        CreateRenderContext.FrameStats frame = CreateRenderContext.lastFrame();
        source.sendSuccess(new TextComponent(
                "[VRO] Create backend " + Backend.getBackendType()
                        + "; loaded contraptions " + contraptions
                        + "; blocks " + blocks + " (rendered " + renderedBlocks + ")"
                        + "; largest " + largest
                        + "; special BEs " + specialBlockEntities
                        + "; actors " + actors + "."
        ), false);
        source.sendSuccess(new TextComponent(
                "[VRO] Last frame culled BEs " + frame.blockEntitiesCulled() + "/" + frame.blockEntitiesTested()
                        + ", actors " + frame.actorsCulled() + "/" + frame.actorsTested()
                        + ", mesh sections " + frame.sectionsCulled() + "/" + frame.sectionsTested()
                        + "; empty flushes skipped " + frame.emptyFlushesSkipped()
                        + "; section threshold " + ClientOptimizationConfig.createSectionedMeshThreshold + "."
        ), false);
        source.sendSuccess(new TextComponent(
                "[VRO] Flywheel automatic instancing "
                        + (ClientOptimizationConfig.createFlywheelAutoEnable ? "ON" : "OFF")
                        + (FlywheelBackendManager.promotedBackend() ? "; restored from OFF this session." : ".")
        ), false);

        if (!Backend.isOn() && largest >= ClientOptimizationConfig.createSectionedMeshThreshold) {
            source.sendSuccess(new TextComponent(
                    "[VRO] Warning: Flywheel is OFF while large contraptions are loaded. Try /flywheel backend instancing."
            ), false);
        }
        return 1;
    }
}
