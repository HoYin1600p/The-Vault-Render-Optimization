package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader;

import com.jozufozu.flywheel.event.RenderLayerEvent;
import net.minecraftforge.common.MinecraftForge;

public final class FlywheelEventsInvoker {
    private FlywheelEventsInvoker() {
    }

    public static void invokeRenderLayer(RenderLayerEvent renderLayerEvent) {
        MinecraftForge.EVENT_BUS.post(renderLayerEvent);
    }
}
