package dev.hoyin1600p.vault_render_optimization.client.shader;

import java.util.HashMap;
import java.util.Map;

/** One render-thread shader instance, invalidated before every load/destroy. */
public final class UniformLocationCache {
    @FunctionalInterface public interface Lookup { int find(int program, String name); }
    private final Map<String, Integer> locations = new HashMap<>();
    private int program = -1;

    public int get(int id, String name, Lookup lookup) {
        if (program != id) {
            clear();
            program = id;
        }
        Integer cached = locations.get(name);
        if (cached != null) return cached;
        int location = lookup.find(id, name);
        // Includes missing uniforms (-1). Bound even untrusted/dynamic uniform names.
        if (locations.size() < 256) locations.put(name, location);
        return location;
    }

    public void clear() { locations.clear(); program = -1; }
}
