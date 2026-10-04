package dev.hoyin1600p.vault_render_optimization.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.common.ForgeConfigSpec;
import org.junit.jupiter.api.Test;

/**
 * Pins the whole client config spec: every path in definition order with its comment, default, range,
 * value class and (for enums) the accepted constants. Refactors of the spec definition must not change it.
 */
class ConfigSpecGoldenTest {
    private static final String GOLDEN = "/config-spec.golden.txt";

    @Test
    void specMatchesGolden() throws IOException {
        String actual = describe();
        String property = System.getenv("VRO_GOLDEN_OUT");
        if (property != null) {
            Files.writeString(Path.of(property), actual, StandardCharsets.UTF_8);
        }
        String expected;
        try (InputStream in = ConfigSpecGoldenTest.class.getResourceAsStream(GOLDEN)) {
            expected = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        assertEquals(expected.replace("\r\n", "\n"), actual);
    }

    static String describe() {
        List<String> lines = new ArrayList<>();
        walk(ClientOptimizationConfig.SPEC.getSpec(), "", lines);
        return String.join("\n", lines) + "\n";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void walk(UnmodifiableConfig config, String prefix, List<String> lines) {
        for (UnmodifiableConfig.Entry entry : config.entrySet()) {
            String path = prefix + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof UnmodifiableConfig child) {
                lines.add("section " + path);
                walk(child, path + ".", lines);
            } else if (value instanceof ForgeConfigSpec.ValueSpec spec) {
                StringBuilder line = new StringBuilder("value ").append(path);
                line.append("\n  comment: ").append(spec.getComment() == null ? "<none>"
                        : spec.getComment().replace("\r", "").replace("\n", " | "));
                line.append("\n  translationKey: ").append(spec.getTranslationKey());
                line.append("\n  restart: ").append(spec.needsWorldRestart());
                line.append("\n  class: ").append(spec.getClazz() == null ? "<none>" : spec.getClazz().getName());
                line.append("\n  default: ").append(spec.getDefault());
                ForgeConfigSpec.Range<?> range = spec.getRange();
                line.append("\n  range: ").append(range == null ? "<none>" : range.getMin() + ".." + range.getMax());
                Class<?> clazz = spec.getClazz();
                if (clazz != null && clazz.isEnum()) {
                    List<String> accepted = new ArrayList<>();
                    for (Object constant : clazz.getEnumConstants()) {
                        accepted.add(constant + "=" + spec.test(constant));
                    }
                    line.append("\n  enum: ").append(String.join(",", accepted));
                }
                lines.add(line.toString());
            } else {
                lines.add("other " + path + " " + value);
            }
        }
    }
}
