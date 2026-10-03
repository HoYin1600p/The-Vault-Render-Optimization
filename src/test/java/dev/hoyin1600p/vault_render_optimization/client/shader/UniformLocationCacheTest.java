package dev.hoyin1600p.vault_render_optimization.client.shader;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UniformLocationCacheTest {
    @Test void cachesMissingUniformsAndInvalidatesForReusedProgramIds() {
        var cache = new UniformLocationCache();
        var calls = new AtomicInteger();
        for (int i = 0; i < 100; i++) assertEquals(-1, cache.get(7, "missing", (id, name) -> {
            calls.incrementAndGet(); return -1;
        }));
        assertEquals(1, calls.get());
        cache.clear(); // relink may reuse exactly the same OpenGL integer name
        assertEquals(3, cache.get(7, "missing", (id, name) -> 3));
        assertEquals(4, cache.get(8, "missing", (id, name) -> 4));
    }
}
