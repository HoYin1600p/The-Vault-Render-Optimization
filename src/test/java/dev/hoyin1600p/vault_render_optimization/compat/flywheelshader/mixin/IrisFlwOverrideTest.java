package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.mixin;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class IrisFlwOverrideTest {
    /** Same shape as Mixin 0.8.5's MixinConfig: private lists, one final. */
    @SuppressWarnings("unused")
    private static final class ConfigShape {
        private List<String> mixinClasses = new ArrayList<>(List.of("IrisHandlerMixin"));
        private final List<String> mixinClassesClient = new ArrayList<>(List.of("MixinProgramCompiler"));
        private List<String> mixinClassesServer = null;
        private final List<String> unrelated = new ArrayList<>(List.of("kept"));
    }

    @Test
    void emptiesExactlyTheThreeMixinListsIncludingFinalAndNullOnes() throws Exception {
        ConfigShape config = new ConfigShape();
        IrisFlwOverride.clearLists(IrisFlwOverride.unsafe(), config);
        assertEquals(List.of(), config.mixinClasses);
        assertEquals(List.of(), config.mixinClassesClient);
        assertEquals(List.of(), config.mixinClassesServer);
        assertEquals(List.of("kept"), config.unrelated);
    }

    @Test
    void missingFieldsAreReportedInsteadOfIgnored() {
        assertThrows(NoSuchFieldException.class, () -> IrisFlwOverride.clearLists(IrisFlwOverride.unsafe(), new Object()));
    }

    @Test
    void withoutRegisteredIrisFlwConfigsTheOverrideReportsFailure() {
        // Outside a Mixin environment there are no irisflw configs: the caller must then disable VRO's copy.
        var result = IrisFlwOverride.disableIrisFlwMixins();
        assertFalse(result.success());
    }
}
