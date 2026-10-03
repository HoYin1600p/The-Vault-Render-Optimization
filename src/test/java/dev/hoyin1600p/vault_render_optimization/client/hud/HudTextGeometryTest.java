package dev.hoyin1600p.vault_render_optimization.client.hud;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class HudTextGeometryTest {
    @Test void rejectsFormattingObfuscationUnicodeAndLongLabels() {
        assertTrue(HudTextGeometry.eligible("Mana 100/150"));
        assertFalse(HudTextGeometry.eligible("\u00a7kSecret"));
        assertFalse(HudTextGeometry.eligible("\u00a7cColor"));
        assertFalse(HudTextGeometry.eligible("\u0627"));
        assertFalse(HudTextGeometry.eligible("x".repeat(65)));
        assertFalse(HudTextGeometry.eligible("line\nline"));
        assertFalse(HudTextGeometry.eligible(null));
    }

    @Test void changingLiveInputsCannotReusePreviousKey() {
        var pose = new Matrix4f(); pose.setIdentity();
        var baseline = new HudTextGeometry.Key("100", 1, 2, -1, true, false, 0, 0, pose.copy());
        assertEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, true, false, 0, 0, pose.copy()));
        assertNotEquals(baseline, new HudTextGeometry.Key("99", 1, 2, -1, true, false, 0, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 2, 2, -1, true, false, 0, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, 123, true, false, 0, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, false, false, 0, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, true, true, 0, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, true, false, 1, 0, pose));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, true, false, 0, 15, pose));
        pose.multiply(Matrix4f.createScaleMatrix(2, 2, 1));
        assertNotEquals(baseline, new HudTextGeometry.Key("100", 1, 2, -1, true, false, 0, 0, pose));
    }

    @Test void replaysExactShadowVertexAttributesWithoutFluentAssumptions() {
        var mesh = new HudGlyphMesh();
        var capture = mesh.getBuffer(null);
        capture.vertex(2.5, 3.5, 0.03).color(12, 34, 56, 78).uv(.25f, .75f).uv2(160, 240).endVertex();
        mesh.freeze();
        var output = mock(VertexConsumer.class);
        mesh.render(type -> output);
        verify(output).vertex(2.5, 3.5, (double) (float) .03);
        verify(output).color(12, 34, 56, 78);
        verify(output).uv(.25f, .75f);
        verify(output).uv2(160, 240);
        verify(output).endVertex();
        assertEquals(44, mesh.bytes());
        assertThrows(IllegalStateException.class, () -> mesh.getBuffer(null));
    }

    @Test void rejectsUnknownOutputBeforeRealBuffersAreTouchedAndBoundsCapture() {
        var capture = new HudGlyphMesh().getBuffer(null);
        assertThrows(UnsupportedOperationException.class, () -> capture.normal(0, 1, 0));
        assertThrows(UnsupportedOperationException.class, capture::endVertex);
        for (int i = 0; i < 512; i++) capture.vertex(0, 0, 0).color(255, 255, 255, 255).uv(0, 0).uv2(0, 0).endVertex();
        assertThrows(UnsupportedOperationException.class,
                () -> capture.vertex(0, 0, 0).color(255, 255, 255, 255).uv(0, 0).uv2(0, 0).endVertex());
    }
}
