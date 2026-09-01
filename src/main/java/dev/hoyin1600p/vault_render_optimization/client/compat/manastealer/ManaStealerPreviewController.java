package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

public final class ManaStealerPreviewController {
    static final float RADIUS = 6.0F;
    static final int AMBIENT_SOUND_TICK = 100;

    private static int nextSourceId = Integer.MIN_VALUE;
    private static Preview active;

    private ManaStealerPreviewController() {
    }

    public static StartResult start(int durationTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return StartResult.failure("No client player is loaded.");
        }
        return start(durationTicks, targetCenter(minecraft));
    }

    public static StartResult start(int durationTicks, Vec3 center) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null) {
            return StartResult.failure("No client world is loaded.");
        }
        if (!ManaStealerVisualController.canPreview()) {
            return StartResult.failure("The Mana Stealer particle sprites are not ready.");
        }
        int sourceId = nextSourceId++;
        active = new Preview(level, sourceId, center, durationTicks);
        play(level, center, SoundEvents.BEACON_ACTIVATE);
        return StartResult.success(center, durationTicks);
    }

    public static boolean stop() {
        boolean wasActive = active != null;
        active = null;
        return wasActive;
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || active == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Preview preview = active;
        if (minecraft.level != preview.level) {
            active = null;
            return;
        }

        ManaStealerVisualController.maintainPreview(
                preview.sourceId,
                preview.level,
                preview.center.x,
                preview.center.y,
                preview.center.z,
                RADIUS
        );
        preview.age++;
        if (preview.age == AMBIENT_SOUND_TICK) {
            play(preview.level, preview.center, SoundEvents.BEACON_AMBIENT);
        }
        if (preview.age >= preview.durationTicks) {
            active = null;
        }
    }

    private static Vec3 targetCenter(Minecraft minecraft) {
        HitResult hitResult = minecraft.hitResult;
        if (hitResult instanceof BlockHitResult blockHit && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos block = blockHit.getBlockPos();
            return new Vec3(block.getX() + 0.5D, block.getY() + 1.0D, block.getZ() + 0.5D);
        }
        return minecraft.player.getEyePosition().add(minecraft.player.getViewVector(1.0F).scale(4.0D));
    }

    private static void play(ClientLevel level, Vec3 center, net.minecraft.sounds.SoundEvent sound) {
        level.playLocalSound(
                center.x,
                center.y,
                center.z,
                sound,
                SoundSource.BLOCKS,
                1.0F,
                0.5F,
                false
        );
    }

    private static final class Preview {
        private final ClientLevel level;
        private final int sourceId;
        private final Vec3 center;
        private final int durationTicks;
        private int age;

        private Preview(ClientLevel level, int sourceId, Vec3 center, int durationTicks) {
            this.level = level;
            this.sourceId = sourceId;
            this.center = center;
            this.durationTicks = durationTicks;
        }
    }

    public record StartResult(boolean started, String error, Vec3 center, int durationTicks) {
        private static StartResult success(Vec3 center, int durationTicks) {
            return new StartResult(true, "", center, durationTicks);
        }

        private static StartResult failure(String error) {
            return new StartResult(false, error, Vec3.ZERO, 0);
        }
    }
}
