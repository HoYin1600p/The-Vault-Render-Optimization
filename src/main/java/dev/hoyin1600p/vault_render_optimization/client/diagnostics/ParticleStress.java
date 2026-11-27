package dev.hoyin1600p.vault_render_optimization.client.diagnostics;

import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Locale;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
 * exists so in-game benchmarks can replay identical particle storms with and without VRO's paths.
 */
public final class ParticleStress {
    public enum Kind { NOVA, FROST }

    private static final Random POSITIONS = new Random(1600);
    private static MethodHandle legacyNovaSpawn;
    private static MethodHandle vault22IsClassic;
    private static MethodHandle vault22ClassicFireNova;
    private static MethodHandle vault22ModernFireNova;
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
        if (requested == Kind.NOVA && !hasNovaSpawner()) {
            return "Vault's Nova particle spawner is not available";
        }
        if (requested == Kind.FROST && particle(ModIds.THE_VAULT, "nova_speed") == null) {
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
            if (kind == Kind.NOVA) castNova(minecraft.level, at);
            else castFrost(minecraft.getConnection(), at);
            castsDone++;
        }
    }

    private static void castNova(ClientLevel level, Vec3 at) {
        try {
            // NovaAbility sends the caster position raised by 0.15.
            Vec3 origin = at.add(0, 0.15, 0);
            resolveNovaSpawner();
            if (legacyNovaSpawn != null) {
                legacyNovaSpawn.invokeExact(origin, radius);
            } else if (vault22ClassicFireNova != null && vault22ModernFireNova != null) {
                boolean classic = vault22IsClassic != null && (boolean) vault22IsClassic.invokeExact();
                if (classic) {
                    vault22ClassicFireNova.invokeExact(level, origin, radius);
                } else {
                    vault22ModernFireNova.invokeExact(level, origin, radius);
                }
            } else {
                ticksLeft = 0;
            }
        } catch (Throwable failure) {
            ticksLeft = 0;
            throw new IllegalStateException("Nova stress spawn failed", failure);
        }
    }

    private static void castFrost(ClientPacketListener connection, Vec3 at) {
        // NovaSpeedAbility: sendParticles(NOVA_SPEED, count, r/2, 0.25, r/2, speed 0), count clamp(pi r^2 * 100, 50, 400).
        int count = (int) Math.max(50, Math.min(400, Math.PI * radius * radius * 100));
        @SuppressWarnings("unchecked")
        ParticleType<?> type = particle(ModIds.THE_VAULT, "nova_speed");
        ClientboundLevelParticlesPacket packet = new ClientboundLevelParticlesPacket((ParticleOptions) type, true,
                at.x, at.y, at.z, radius / 2, 0.25F, radius / 2, 0.0F, count);
        connection.handleParticleEvent(packet);
    }

    private static ParticleType<?> particle(String namespace, String path) {
        ParticleType<?> type = Registry.PARTICLE_TYPE.get(new ResourceLocation(namespace, path));
        return type instanceof ParticleOptions ? type : null;
    }

    private static boolean hasNovaSpawner() {
        resolveNovaSpawner();
        return legacyNovaSpawn != null || (vault22ClassicFireNova != null && vault22ModernFireNova != null);
    }

    private static void resolveNovaSpawner() {
        if (novaResolved) {
            return;
        }
        novaResolved = true;
        MethodHandles.Lookup lookup = MethodHandles.publicLookup();
        try {
            Class<?> message = Class.forName("iskallia.vault.network.message.NovaParticleMessage");
            legacyNovaSpawn = lookup.findStatic(message, "spawnParticles",
                    MethodType.methodType(void.class, Vec3.class, float.class));
            return;
        } catch (ReflectiveOperationException | LinkageError ignored) {
            legacyNovaSpawn = null;
        }
        try {
            Class<?> classic = Class.forName("iskallia.vault.client.particles.ClassicAbilityParticles");
            Class<?> modern = Class.forName("iskallia.vault.client.particles.NovaWaveParticles");
            MethodType spawnType = MethodType.methodType(void.class, ClientLevel.class, Vec3.class, float.class);
            vault22ClassicFireNova = lookup.findStatic(classic, "spawnFireNova", spawnType);
            vault22ModernFireNova = lookup.findStatic(modern, "spawnFireNova", spawnType);
            try {
                vault22IsClassic = lookup.findStatic(classic, "isClassic", MethodType.methodType(boolean.class));
            } catch (ReflectiveOperationException | LinkageError ignored) {
                vault22IsClassic = null;
            }
        } catch (ReflectiveOperationException | LinkageError failure) {
            vault22ClassicFireNova = null;
            vault22ModernFireNova = null;
            vault22IsClassic = null;
        }
    }
}
