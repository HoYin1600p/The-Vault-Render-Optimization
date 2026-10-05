package dev.hoyin1600p.vault_render_optimization.client.benchmark;

import dev.hoyin1600p.vault_render_optimization.client.benchmark.scene.VirtualScene;
import dev.hoyin1600p.vault_render_optimization.client.config.VroScreenScale;
import dev.hoyin1600p.vault_render_optimization.util.CommandText;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.world.WorldEvent;

/** All commands and callbacks run on the client render thread. Idle cost is one volatile read. */
public final class BenchmarkFrameHook {
    private static volatile BenchmarkRunner active;
    private static BenchmarkRunner lastRun;
    private static BenchmarkDisplaySettings displaySettings;
    private static ClientLevel benchmarkLevel;
    private static boolean sceneRun;
    private static Float originalPitch;
    // The player can neither move nor look around during a run; the view is held here.
    private static float heldYaw;
    private static float heldPitch;

    private BenchmarkFrameHook() { }

    public static String start() {
        return start(true);
    }

    public static String start(boolean crowdedScene) {
        if (active != null) return "A GPU benchmark is already running.";
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) return "Cannot benchmark: no world is loaded.";
        if (!minecraft.player.isAlive()) return "Cannot benchmark while the player is dead.";
        BenchmarkRunner runner = new BenchmarkRunner(GpuBenchmarkToggles.create(), System::nanoTime);
        try {
            displaySettings = new BenchmarkDisplaySettings(new BenchmarkDisplaySettings.Options() {
                public boolean vsync() { return minecraft.options.enableVsync; }
                public int optionLimit() { return minecraft.options.framerateLimit; }
                public int windowLimit() { return minecraft.getWindow().getFramerateLimit(); }
                public void apply(boolean vsync, int optionLimit, int windowLimit) {
                    minecraft.options.enableVsync = vsync;
                    minecraft.options.framerateLimit = optionLimit;
                    minecraft.getWindow().updateVsync(vsync);
                    minecraft.getWindow().setFramerateLimit(windowLimit);
                }
            });
            benchmarkLevel = minecraft.level;
            sceneRun = crowdedScene;
            if (sceneRun) {
                VirtualScene.spawn(benchmarkLevel, minecraft.player.getEyePosition(), minecraft.player.getYRot());
                aimAtScene(minecraft);
            }
            heldYaw = minecraft.player.getYRot();
            heldPitch = minecraft.player.getXRot();
            runner.start();
            lastRun = runner;
            if (!runner.active()) {
                cleanup();
                return "No GPU toggles are available; use /vro benchmark gpu result for skipped reasons.";
            }
            active = runner;
            BenchmarkPlan plan = BenchmarkPlan.DEFAULT;
            return "GPU benchmark started " + (sceneRun ? "with a client-only crowded scene." : "in the current view.")
                    + String.format(Locale.ROOT, " All GPU paths first, then each switch: %.0f s warmup + %.0f s"
                            + " measurement per round (about %.0f s; retries may add time).",
                            plan.warmupSeconds(), plan.measureSeconds(), runner.estimatedTotalSeconds())
                    + " You cannot move or look around until it ends; opening a screen pauses and restarts the round.";
        } catch (RuntimeException | Error failure) {
            try { runner.cancel(); } finally { cleanup(); }
            throw failure;
        }
    }

    public static String cancel() {
        BenchmarkRunner runner = active;
        if (runner == null) return "No GPU benchmark is running.";
        try { runner.cancel(); } finally {
            active = null;
            cleanup();
        }
        return "GPU benchmark cancelled; original toggles and display settings restored.";
    }

    public static void onRenderTick(TickEvent.RenderTickEvent event) {
        BenchmarkRunner runner = active;
        if (runner == null || event.phase != TickEvent.Phase.START) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (invalidWorld(minecraft)) {
            cancel();
            return;
        }
        holdView(minecraft);
        try {
            runner.onFrame(System.nanoTime(), minecraft.isWindowActive(), minecraft.screen != null);
        } catch (RuntimeException | Error failure) {
            cancel();
            throw failure;
        } finally {
            if (!runner.active()) {
                active = null;
                cleanup();
                if (runner.phase() == BenchmarkRunner.Phase.COMPLETE) {
                    minecraft.setScreen(VroScreenScale.own(BenchmarkResultScreen.create(runner)));
                }
            }
        }
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (active == null) return;
        if (event.phase == TickEvent.Phase.START) {
            // Movement keys are read during the tick; released here, the player stays put.
            KeyMapping.releaseAll();
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (invalidWorld(minecraft)) {
            cancel();
            return;
        }
        try {
            if (sceneRun) VirtualScene.tick(active.phase() != BenchmarkRunner.Phase.PAUSED
                    && minecraft.isWindowActive() && minecraft.screen == null);
        } catch (RuntimeException | Error failure) {
            cancel();
            throw failure;
        }
    }

    public static void onWorldUnload(WorldEvent.Unload event) {
        if (benchmarkLevel != null && event.getWorld() == benchmarkLevel) cancel();
    }

    public static void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        cancel();
    }

    private static boolean invalidWorld(Minecraft minecraft) {
        return minecraft.level != benchmarkLevel || minecraft.player == null || !minecraft.player.isAlive();
    }

    /** The camera looks at the scene's centre for the run, whatever the terrain; the pitch is restored afterwards. */
    private static void aimAtScene(Minecraft minecraft) {
        Vec3 centre = VirtualScene.centre();
        if (centre == null || minecraft.player == null) return;
        Vec3 eye = minecraft.player.getEyePosition();
        double dy = centre.y - eye.y;
        double horizontal = Math.hypot(centre.x - eye.x, centre.z - eye.z);
        float pitch = (float) Math.toDegrees(-Math.atan2(dy, Math.max(horizontal, 1e-3)));
        originalPitch = minecraft.player.getXRot();
        minecraft.player.setXRot(pitch);
        minecraft.player.xRotO = pitch;
    }

    /** Runs after the mouse has turned the player and before the frame is drawn, so the view never moves. */
    private static void holdView(Minecraft minecraft) {
        if (minecraft.player == null) return;
        minecraft.player.setYRot(heldYaw);
        minecraft.player.yRotO = heldYaw;
        minecraft.player.setXRot(heldPitch);
        minecraft.player.xRotO = heldPitch;
    }

    private static void restorePitch() {
        Float pitch = originalPitch;
        originalPitch = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (pitch != null && minecraft.player != null) {
            minecraft.player.setXRot(pitch);
            minecraft.player.xRotO = pitch;
        }
    }

    private static void cleanup() {
        benchmarkLevel = null;
        sceneRun = false;
        try { VirtualScene.clear(); } finally {
            try { restorePitch(); } finally { restoreDisplay(); }
        }
    }

    private static void restoreDisplay() {
        if (displaySettings != null) {
            displaySettings.close();
            displaySettings = null;
        }
    }

    public static String status() {
        BenchmarkRunner runner = lastRun;
        if (runner == null) return "GPU benchmark: IDLE, toggle -, round -, progress 0%.";
        BenchmarkStep step = runner.step();
        return String.format(Locale.ROOT, "GPU benchmark: %s, toggle %s, round %s, progress %.1f%%.",
                runner.phase(), step == null ? "-" : step.displayName(),
                step == null ? "-" : step.round() + "/" + BenchmarkPlan.ROUNDS, runner.progressPercent());
    }

    public static List<String> resultLines() {
        if (lastRun == null) return List.of("No GPU benchmark results yet.");
        List<String> lines = new ArrayList<>();
        lines.add(status());
        for (BenchmarkRunner.ToggleResult result : lastRun.results()) {
            lines.add(verdictLine(result.displayName(), result.verdict()));
            for (int i = 0; i < result.rounds().size(); i++) {
                FrameStats stats = result.rounds().get(i);
                lines.add(String.format(Locale.ROOT,
                        "  Round %d (%s): average FPS %.1f, median %.2f ms, p99 %.2f ms, 1%% low %.1f FPS",
                        i + 1, CommandText.onOff(BenchmarkPlan.enabled(i)), stats.averageFps(),
                        stats.medianMs(), stats.p99Ms(), stats.onePercentLowFps()));
            }
        }
        if (lastRun.recommendations() != null) {
            lastRun.recommendations().states().forEach((id, enabled) -> lines.add(id + ": recommended "
                    + CommandText.onOff(enabled) + (lastRun.recommendations().required().contains(id)
                    ? " (required by a dependent GPU switch)" : "")));
        }
        if (lastRun.noChangeRecommended()) lines.add("No change recommended; CONFIRM skipped.");
        if (lastRun.confirmation() != null) {
            lines.add(verdictLine("CONFIRM (ON = recommended, OFF = original)", lastRun.confirmation()));
        }
        for (BenchmarkRunner.SkippedToggle skipped : lastRun.skipped()) {
            lines.add(skipped.displayName() + ": skipped - " + skipped.reason());
        }
        return List.copyOf(lines);
    }

    private static String verdictLine(String name, BenchmarkVerdict verdict) {
        return String.format(Locale.ROOT, "%s: median FPS on %.1f, off %.1f; average FPS on %.1f, off %.1f;"
                        + " delta %+.2f%%, noise %.2f%%, %s%s",
                name, verdict.medianFpsOn(), verdict.medianFpsOff(), verdict.meanFpsOn(), verdict.meanFpsOff(),
                verdict.deltaPercent(), verdict.noisePercent(), verdict.recommendation(),
                verdict.noisy() ? " (noisy)" : "");
    }
}
