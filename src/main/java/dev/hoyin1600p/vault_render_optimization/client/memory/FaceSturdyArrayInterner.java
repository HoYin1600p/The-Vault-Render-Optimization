package dev.hoyin1600p.vault_render_optimization.client.memory;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class FaceSturdyArrayInterner {
    private static final ConcurrentMap<BooleanArrayKey, boolean[]> CANONICAL_ARRAYS =
            new ConcurrentHashMap<>();

    private FaceSturdyArrayInterner() {
    }

    public static boolean[] intern(boolean[] candidate) {
        BooleanArrayKey key = new BooleanArrayKey(candidate);
        return CANONICAL_ARRAYS.computeIfAbsent(key, ignored -> candidate);
    }

    private static final class BooleanArrayKey {
        private final boolean[] values;
        private final int hashCode;

        private BooleanArrayKey(boolean[] values) {
            this.values = values;
            this.hashCode = Arrays.hashCode(values);
        }

        @Override
        public boolean equals(Object other) {
            return this == other
                    || other instanceof BooleanArrayKey key
                    && Arrays.equals(this.values, key.values);
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
