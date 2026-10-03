package dev.hoyin1600p.vault_render_optimization.client.create;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * IdentityHashMap entries throw once their iterator removed them, which crashed the client when a dead
 * Create contraption's fallback meshes were freed. No main source may read an entry after removing it.
 */
class IteratorRemoveOrderTest {
    private static final Pattern REMOVE_THEN_READ =
            Pattern.compile("(\\w+)\\.remove\\(\\);\\s*\\n\\s*[^\\n]*\\b(\\w+)\\.get(Value|Key)\\(\\)");

    @Test
    void identityHashMapEntryIsUnusableAfterIteratorRemove() {
        Map<Object, String> map = new IdentityHashMap<>();
        map.put(new Object(), "mesh");
        Iterator<Map.Entry<Object, String>> entries = map.entrySet().iterator();
        Map.Entry<Object, String> entry = entries.next();
        entries.remove();
        assertThrows(IllegalStateException.class, entry::getValue);
    }

    @Test
    void noMainSourceReadsAnEntryAfterRemovingIt() throws IOException {
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main", "java"))) {
            for (Path file : (Iterable<Path>) files.filter(p -> p.toString().endsWith(".java"))::iterator) {
                Matcher matcher = REMOVE_THEN_READ.matcher(Files.readString(file).replace("\r\n", "\n"));
                while (matcher.find()) {
                    offenders.add(file.getFileName() + ": " + matcher.group().trim().replaceAll("\\s+", " "));
                }
            }
        }
        assertTrue(Files.isDirectory(Path.of("src", "main", "java")));
        assertEquals(List.of(), offenders);
    }
}
