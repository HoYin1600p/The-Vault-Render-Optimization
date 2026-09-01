package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AsyncBufferArenaMixinSignatureTest {
    private static final Path SOURCE = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/backport/embeddium/buffer/AsyncBufferArenaMixin.java"
    );

    @Test
    void modifyArgHandlerOnlyAcceptsTheSelectedResizeCapacity() throws IOException {
        String source = Files.readString(SOURCE);

        assertTrue(source.contains(
                "private int vro$preemptiveBoundedGrowth(int originalCapacity)"
        ));
    }

    @Test
    void headInjectionCapturesTheExactEnsureCapacityArguments() throws IOException {
        String source = Files.readString(SOURCE);
        int handlerStart = source.indexOf("private void vro$captureRequestedElements(");
        int handlerEnd = source.indexOf(") {", handlerStart);

        assertTrue(handlerStart >= 0);
        assertTrue(handlerEnd > handlerStart);
        String signature = source.substring(handlerStart, handlerEnd);
        assertEquals(1, count(signature, "CommandList commandList"));
        assertEquals(1, count(signature, "int requestedElements"));
        assertEquals(1, count(signature, "CallbackInfo callback"));
    }

    private static int count(String value, String fragment) {
        return (value.length() - value.replace(fragment, "").length()) / fragment.length();
    }
}
