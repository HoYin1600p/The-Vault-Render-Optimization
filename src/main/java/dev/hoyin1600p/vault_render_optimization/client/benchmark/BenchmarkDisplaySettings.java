package dev.hoyin1600p.vault_render_optimization.client.benchmark;

/** Captures both the saved option and live window limit; neither operation saves options. */
final class BenchmarkDisplaySettings implements AutoCloseable {
    interface Options {
        boolean vsync();
        int optionLimit();
        int windowLimit();
        void apply(boolean vsync, int optionLimit, int windowLimit);
    }

    private final Options options;
    private final boolean vsync;
    private final int optionLimit;
    private final int windowLimit;
    private boolean closed;

    BenchmarkDisplaySettings(Options options) {
        this.options = options;
        vsync = options.vsync();
        optionLimit = options.optionLimit();
        windowLimit = options.windowLimit();
        try {
            options.apply(false, 260, 260);
        } catch (RuntimeException | Error failure) {
            try {
                options.apply(vsync, optionLimit, windowLimit);
            } catch (RuntimeException | Error restoreFailure) {
                failure.addSuppressed(restoreFailure);
            }
            throw failure;
        }
    }

    public void close() {
        if (!closed) {
            closed = true;
            options.apply(vsync, optionLimit, windowLimit);
        }
    }
}
