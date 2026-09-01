package dev.hoyin1600p.vault_render_optimization.client.compat.manastealer;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public final class ManaStealerVisualConfig {
    public static final ForgeConfigSpec SPEC;

    private static final ForgeConfigSpec.BooleanValue ENABLED;
    private static final ForgeConfigSpec.BooleanValue REPLACE_GROUND_SIGIL;
    private static final ForgeConfigSpec.IntValue ALL_POPULATION;
    private static final ForgeConfigSpec.IntValue DECREASED_POPULATION;
    private static final ForgeConfigSpec.IntValue MINIMAL_POPULATION;
    private static final ForgeConfigSpec.IntValue MAX_SPAWNS_PER_TICK;
    private static final ForgeConfigSpec.DoubleValue MINIMUM_SPEED;
    private static final ForgeConfigSpec.DoubleValue MAXIMUM_SPEED;
    private static final ForgeConfigSpec.DoubleValue OUTER_DIAMETER;
    private static final ForgeConfigSpec.DoubleValue INNER_RATIO;

    private static volatile boolean enabled = true;
    private static volatile boolean replaceGroundSigil = true;
    private static volatile int allPopulation = 80;
    private static volatile int decreasedPopulation = 52;
    private static volatile int minimalPopulation = 20;
    private static volatile int maxSpawnsPerTick = 8;
    private static volatile double minimumSpeed = 0.125D;
    private static volatile double maximumSpeed = 0.2D;
    private static volatile float outerDiameter = 0.3F;
    private static volatile float innerRatio = 0.46F;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.comment(
                "Experimental, removable Mana Stealer trap visual replacement.",
                "All values apply on the client and leave Vault gameplay unchanged."
        ).push("mana_stealer_visual_prototype");
        ENABLED = builder
                .comment("Replace the Mana Stealer dust with bounded inward-moving composite orbs.")
                .define("enabled", true);
        REPLACE_GROUND_SIGIL = builder
                .comment("Hide Vault's legacy flat line sigil while the orb replacement is active.")
                .define("replace_ground_sigil", true);
        ALL_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's All particle setting.")
                .defineInRange("all_population", 80, 0, 512);
        DECREASED_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's Decreased particle setting.")
                .defineInRange("decreased_population", 52, 0, 512);
        MINIMAL_POPULATION = builder
                .comment("Live composite-orb target for Minecraft's Minimal particle setting.")
                .defineInRange("minimal_population", 20, 0, 512);
        MAX_SPAWNS_PER_TICK = builder
                .comment("Maximum deficit filled each tick; limits the initial visual ramp.")
                .defineInRange("max_spawns_per_tick", 8, 1, 64);
        MINIMUM_SPEED = builder
                .comment("Minimum inward speed in blocks per tick; 0.125 equals 2.5 blocks/second.")
                .defineInRange("minimum_speed", 0.125D, 0.01D, 2.0D);
        MAXIMUM_SPEED = builder
                .comment("Maximum inward speed in blocks per tick; 0.2 equals 4 blocks/second.")
                .defineInRange("maximum_speed", 0.2D, 0.01D, 2.0D);
        OUTER_DIAMETER = builder
                .comment("Starting outer-orb diameter in blocks. It shrinks to ten percent at the center.")
                .defineInRange("outer_diameter", 0.3D, 0.02D, 2.0D);
        INNER_RATIO = builder
                .comment("Inner navy ball diameter as a fraction of the pale outer ball.")
                .defineInRange("inner_diameter_ratio", 0.46D, 0.1D, 0.95D);
        builder.pop();
        SPEC = builder.build();
    }

    private ManaStealerVisualConfig() {
    }

    public static boolean active() {
        return enabled && ClientOptimizationConfig.optimizationsEnabled();
    }

    public static boolean configuredEnabled() {
        return enabled;
    }

    public static boolean replaceGroundSigil() {
        return replaceGroundSigil;
    }

    public static int allPopulation() {
        return allPopulation;
    }

    public static int decreasedPopulation() {
        return decreasedPopulation;
    }

    public static int minimalPopulation() {
        return minimalPopulation;
    }

    public static int maxSpawnsPerTick() {
        return maxSpawnsPerTick;
    }

    public static double minimumSpeed() {
        return minimumSpeed;
    }

    public static double maximumSpeed() {
        return maximumSpeed;
    }

    public static float outerDiameter() {
        return outerDiameter;
    }

    public static float innerRatio() {
        return innerRatio;
    }

    public static void setEnabled(boolean value) {
        ENABLED.set(value);
        ENABLED.save();
        enabled = value;
    }

    public static void setReplaceGroundSigil(boolean value) {
        REPLACE_GROUND_SIGIL.set(value);
        REPLACE_GROUND_SIGIL.save();
        replaceGroundSigil = value;
    }

    public static void onLoading(ModConfigEvent.Loading event) {
        bake(event.getConfig());
    }

    public static void onReloading(ModConfigEvent.Reloading event) {
        bake(event.getConfig());
    }

    private static void bake(ModConfig config) {
        if (config.getSpec() != SPEC) {
            return;
        }
        enabled = ENABLED.get();
        replaceGroundSigil = REPLACE_GROUND_SIGIL.get();
        allPopulation = ALL_POPULATION.get();
        decreasedPopulation = DECREASED_POPULATION.get();
        minimalPopulation = MINIMAL_POPULATION.get();
        maxSpawnsPerTick = MAX_SPAWNS_PER_TICK.get();
        minimumSpeed = Math.min(MINIMUM_SPEED.get(), MAXIMUM_SPEED.get());
        maximumSpeed = Math.max(MINIMUM_SPEED.get(), MAXIMUM_SPEED.get());
        outerDiameter = OUTER_DIAMETER.get().floatValue();
        innerRatio = INNER_RATIO.get().floatValue();
    }
}
