package dev.hoyin1600p.vault_render_optimization.client.particle;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.protocol.game.ClientboundLevelParticlesPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;

/**
 * Client-only benchmark driver that reproduces Vault Hunters Nova casts next to the player through
 * the same client code a real cast runs: Vault's {@code NovaParticleMessage.spawnParticles} for Nova,
 * and a real {@code ClientboundLevelParticlesPacket} handed to the client's own
 * {@code handleParticleEvent} for Frost Nova. Nothing is sent to the server and no sound plays; it
 * exists so CMA benchmarks can replay identical particle storms with and without VRO's paths.
 */
public final class ParticleStress {
    public enum Kind { NOVA, FROST }

    private static final Random POSITIONS = new Random(1600);
    private static MethodHandle novaSpawn;
    private static boolean novaResolved;

    private static Kind kind;
    private static int castsPerSecond;
    private static float radius;
    private static int ticksLeft;
    private static long castsDone;
    private static double castRemainder;

    private ParticleStress() {
    }

    public static String start(Kind requested, int perSecond, int seconds, float castRadius) {
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return "no world is loaded";
        }
        if (requested == Kind.NOVA && novaSpawner() == null) {
            return "Vault's NovaParticleMessage.spawnParticles is not available";
        }
        if (requested == Kind.FROST && particle("the_vault", "nova_speed") == null) {
            return "the_vault:nova_speed is not registered";
        }
        kind = requested;
        castsPerSecond = perSecond;
        radius = castRadius;
        ticksLeft = seconds * 20;
        castsDone = 0;
        castRemainder = 0;
        return String.format(Locale.ROOT, "started %s: %d casts/s for %d s, radius %.1f",
                requested.name().toLowerCase(Locale.ROOT), perSecond, seconds, castRadius);
    }

    public static void stop() {
        ticksLeft = 0;
    }

    public static String status() {
        return ticksLeft > 0
                ? String.format(Locale.ROOT, "running %s, %d casts so far, %.1f s left",
                        kind.name().toLowerCase(Locale.ROOT), castsDone, ticksLeft / 20.0)
                : "idle (" + castsDone + " casts in the last run)";
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ticksLeft <= 0) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || minecraft.getConnection() == null) {
            ticksLeft = 0;
            return;
        }
        ticksLeft--;
        castRemainder += castsPerSecond / 20.0;
        int casts = (int) castRemainder;
        castRemainder -= casts;
        Vec3 feet = minecraft.player.position();
        for (int i = 0; i < casts; i++) {
            // Casters stand within 6 blocks of the player, as in a crowded vault room.
            Vec3 at = feet.add(POSITIONS.nextDouble() * 12 - 6, 0, POSITIONS.nextDouble() * 12 - 6);
            if (kind == Kind.NOVA) castNova(at);
            else castFrost(minecraft.getConnection(), at);
            castsDone++;
        }
    }

    private static void castNova(Vec3 at) {
        try {
            // NovaAbility sends the caster position raised by 0.15.
            novaSpawner().invokeExact(at.add(0, 0.15, 0), radius);
        } catch (Throwable failure) {
            ticksLeft = 0;
            throw new IllegalStateException("Nova stress spawn failed", failure);
        }
    }

    private static void castFrost(ClientPacketListener connection, Vec3 at) {
        // NovaSpeedAbility: sendParticles(NOVA_SPEED, count, r/2, 0.25, r/2, speed 0), count clamp(pi r^2 * 100, 50, 400).
        int count = (int) Math.max(50, Math.min(400, Math.PI * radius * radius * 100));
        @SuppressWarnings("unchecked")
        ParticleType<?> type = particle("the_vault", "nova_speed");
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket((ParticleOptions) type, true,
                at.x, at.y, at.z, radius / 2, 0.25F, radius / 2, 0.0F, count);
        connection.handleParticleEvent(packet);
    }

    private static ParticleType<?> particle(String namespace, String path) {
        ParticleType<?> type = Registry.PARTICLE_TYPE.get(new ResourceLocation(namespace, path));
        return type instanceof ParticleOptions ? type : null;
    }

    private static MethodHandle novaSpawner() {
        if (!novaResolved) {
            novaResolved = true;
            try {
                Class<?> message = Class.forName("iskallia.vault.network.message.NovaParticleMessage");
                novaSpawn = MethodHandles.publicLookup().findStatic(message, "spawnParticles",
                        MethodType.methodType(void.class, Vec3.class, float.class));
            } catch (ReflectiveOperationException | LinkageError failure) {
                novaSpawn = null;
            }
        }
        return novaSpawn;
    }
}
