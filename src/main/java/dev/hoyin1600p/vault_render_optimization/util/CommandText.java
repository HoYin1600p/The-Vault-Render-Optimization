package dev.hoyin1600p.vault_render_optimization.util;

import dev.hoyin1600p.vault_render_optimization.VaultRenderOptimization;

/** Wording shared by the in-game commands. */
public final class CommandText {
    private CommandText() {
    }

    /** The "ON" or "OFF" word the command output uses for a switch. */
    public static String onOff(boolean enabled) {
        return enabled ? "ON" : "OFF";
    }

    /** Mirrors a line of command output into the log so automated runs can read it without the chat overlay. */
    public static void logCommandLine(String line) {
        VaultRenderOptimization.LOGGER.info("[command] {}", line);
    }
}
