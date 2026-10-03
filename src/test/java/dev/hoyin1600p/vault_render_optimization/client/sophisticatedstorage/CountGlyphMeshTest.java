package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix4f;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CountGlyphMeshTest {
    @Test void replaysSameLocalGeometryWithFreshPoseColorAndLightWithoutFluentAssumptions() {
        var mesh = new CountGlyphMesh();
        var capture = mesh.getBuffer(null);
        for (int i = 0; i < 4; i++) {
            capture.vertex(i, 2, 3).color(255, 255, 255, 255).uv(.25f, .75f).uv2(0).endVertex();
            assertSame(capture, mesh.getBuffer(null));
        }
        mesh.freeze();
        var output = mock(VertexConsumer.class); // Fluent calls intentionally return null.
        var matrix = Matrix4f.createTranslateMatrix(10, 20, 30);
        var requests = new AtomicInteger();
        mesh.render(matrix, type -> { requests.incrementAndGet(); return output; }, 0x123456, 0x00F000A0);
        assertEquals(1, requests.get());
        for (int i = 0; i < 4; i++) verify(output).vertex(same(matrix), eq((float)i), eq(2f), eq(3f));
        verify(output, times(4)).color(0x12, 0x34, 0x56, 255);
        verify(output, times(4)).uv(.25f, .75f);
        verify(output, times(4)).uv2(0x00F000A0);
        verify(output, times(4)).endVertex();
        clearInvocations(output);
        mesh.render(matrix, type -> output, 0x80654321, 7);
        verify(output, times(4)).color(0x65, 0x43, 0x21, 0x80);
        verify(output, times(4)).uv2(7);
    }

    @Test void boundsMemoryAndRejectsNonstandardGlyphAttributes() {
        var capture = new CountGlyphMesh().getBuffer(null);
        assertThrows(UnsupportedOperationException.class, () -> capture.color(1, 2, 3, 4));
        assertThrows(UnsupportedOperationException.class, () -> capture.normal(0, 1, 0));
        for (int i = 0; i < 128; i++) capture.endVertex();
        assertThrows(UnsupportedOperationException.class, capture::endVertex);
    }
}
