package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class FluidRendererMixinSignatureTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/backport/embeddium/fluid/FluidRendererMixin.java"
    );

    @Test
    void modifyArgHandlerOnlyAcceptsTheSelectedInvocationArgument() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains(
                "private LightMode vro$selectFluidLighting(LightMode original)"
        ));
    }

    @Test
    void headInjectionCapturesTheExactStockRenderArguments() throws IOException {
        String source = Files.readString(SOURCE);
        int handlerStart = source.indexOf("private void vro$prepareFluidLighting(");
        int handlerEnd = source.indexOf(") {", handlerStart);

        assertTrue(handlerStart >= 0);
        assertTrue(handlerEnd > handlerStart);
        String signature = source.substring(handlerStart, handlerEnd);
        assertEquals(1, count(signature, "BlockAndTintGetter world"));
        assertEquals(1, count(signature, "FluidState fluidState"));
        assertEquals(2, count(signature, "BlockPos "));
        assertEquals(1, count(signature, "ChunkModelBuilder buffers"));
        assertEquals(1, count(signature, "CallbackInfoReturnable<Boolean> callback"));
    }

    private static int count(String value, String fragment) {
        return (value.length() - value.replace(fragment, "").length()) / fragment.length();
    }
}
