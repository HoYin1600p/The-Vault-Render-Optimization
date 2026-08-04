package dev.hoyin1600p.vault_render_optimization.client.lighting;

import dev.hoyin1600p.vault_render_optimization.config.ClientOptimizationConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.projectile.AbstractHurtingProjectile;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public final class DynamicLightEngine {
    private static final double MAX_RADIUS = 7.75D;
    private static final double MAX_RADIUS_SQUARED = MAX_RADIUS * MAX_RADIUS;
    private static final IdentityHashMap<Object, SourceState> SOURCES = new IdentityHashMap<>();
    private static final Long2ObjectOpenHashMap<ArrayList<SourceState>> CELLS = new Long2ObjectOpenHashMap<>();
    private static final LongOpenHashSet PENDING_REBUILDS = new LongOpenHashSet();
    private static final boolean STANDALONE_DYNAMIC_LIGHTS = ModList.get().isLoaded("dynamiclightsreforged");

    private static ClientLevel world;
    private static boolean shaderBlocked;
    private static long queries;
    private static long candidates;
    private static int updatesLastTick;
    private static int updatesThisTick;
    private static int rebuildsLastTick;

    private DynamicLightEngine() {
    }

    public static void observeEntity(Entity entity) {
        if (!(entity.level instanceof ClientLevel clientLevel) || !active()) {
            return;
        }
        int luminance = ClientOptimizationConfig.dynamicLightEntities ? entityLuminance(entity) : 0;
        observe(entity, clientLevel, entity.getX(), entity.getEyeY(), entity.getZ(), luminance);
    }

    public static void removeEntity(Entity entity) {
        remove(entity);
    }

    public static void observeBlockEntity(ClientLevel level, BlockEntity blockEntity) {
        if (!shouldObserveBlockEntities()) {
            return;
        }
        BlockPos pos = blockEntity.getBlockPos();
        int luminance = DynamicLightResourceLoader.INSTANCE.blockEntityLuminance(blockEntity);
        observe(blockEntity, level, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, luminance);
    }

    public static boolean shouldObserveBlockEntities() {
        return active() && ClientOptimizationConfig.dynamicLightBlockEntities;
    }

    private static void observe(
            Object owner,
            ClientLevel level,
            double x,
            double y,
            double z,
            int luminance
    ) {
        ensureWorld(level);
        long tick = level.getGameTime();
        SourceState source = SOURCES.get(owner);
        if (source == null) {
            if (luminance <= 0) {
                return;
            }
            source = new SourceState(owner);
            source.nextUpdateTick = tick;
            SOURCES.put(owner, source);
        }
        source.lastSeenTick = tick;

        if (tick < source.nextUpdateTick) {
            return;
        }
        source.nextUpdateTick = tick + ClientOptimizationConfig.dynamicLightUpdateInterval;

        luminance = Mth.clamp(luminance, 0, 15);
        if (source.initialized
                && Math.abs(source.x - x) <= 0.1D
                && Math.abs(source.y - y) <= 0.1D
                && Math.abs(source.z - z) <= 0.1D
                && source.luminance == luminance) {
            return;
        }

        LongOpenHashSet oldSections = source.litSections;
        removeFromCell(source);
        source.x = x;
        source.y = y;
        source.z = z;
        source.luminance = luminance;
        source.initialized = true;
        source.litSections = affectedSections(x, y, z, luminance);
        if (luminance > 0) {
            addToCell(source);
        }
        PENDING_REBUILDS.addAll(oldSections);
        PENDING_REBUILDS.addAll(source.litSections);
        updatesThisTick++;

        if (luminance <= 0) {
            SOURCES.remove(owner);
        }
    }

    public static int applyPackedLight(BlockPos pos, int vanilla) {
        double dynamic = dynamicLightLevel(pos);
        int block = LightTexture.block(vanilla);
        if (dynamic <= block) {
            return vanilla;
        }
        int encoded = Mth.clamp((int) (dynamic * 16.0D), 0, 240);
        return vanilla & 0xfff00000 | encoded;
    }

    public static int applyEntityBlockLight(Entity entity, BlockPos pos, int vanilla) {
        if (!active()) {
            return vanilla;
        }
        SourceState source = SOURCES.get(entity);
        int ownLight = source == null ? 0 : source.luminance;
        return Math.max(vanilla, Math.max(ownLight, (int) dynamicLightLevel(pos)));
    }

    public static double dynamicLightLevel(BlockPos pos) {
        if (!active() || world == null) {
            return 0.0D;
        }

        queries++;
        int cellX = SectionPos.blockToSectionCoord(pos.getX());
        int cellY = SectionPos.blockToSectionCoord(pos.getY());
        int cellZ = SectionPos.blockToSectionCoord(pos.getZ());
        double result = 0.0D;

        for (int x = cellX - 1; x <= cellX + 1; x++) {
            for (int y = cellY - 1; y <= cellY + 1; y++) {
                for (int z = cellZ - 1; z <= cellZ + 1; z++) {
                    ArrayList<SourceState> cell = CELLS.get(SectionPos.asLong(x, y, z));
                    if (cell == null) {
                        continue;
                    }
                    candidates += cell.size();
                    for (SourceState source : cell) {
                        double dx = pos.getX() + 0.5D - source.x;
                        double dy = pos.getY() + 0.5D - source.y;
                        double dz = pos.getZ() + 0.5D - source.z;
                        double distanceSquared = dx * dx + dy * dy + dz * dz;
                        if (distanceSquared <= MAX_RADIUS_SQUARED) {
                            double light = (1.0D - Math.sqrt(distanceSquared) / MAX_RADIUS) * source.luminance;
                            result = Math.max(result, light);
                        }
                    }
                }
            }
        }
        return Mth.clamp(result, 0.0D, 15.0D);
    }

    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        shaderBlocked = !ClientOptimizationConfig.dynamicLightsWithShaders && ShaderState.shadersActive();
        Minecraft minecraft = Minecraft.getInstance();
        if (!active() || minecraft.level == null) {
            clear();
            return;
        }

        ensureWorld(minecraft.level);
        long tick = minecraft.level.getGameTime();
        Iterator<Map.Entry<Object, SourceState>> iterator = SOURCES.entrySet().iterator();
        while (iterator.hasNext()) {
            SourceState source = iterator.next().getValue();
            if (source.lastSeenTick < tick - 1L) {
                removeFromCell(source);
                PENDING_REBUILDS.addAll(source.litSections);
                iterator.remove();
            }
        }
        flushRebuilds(minecraft.levelRenderer);
    }

    public static void clearIfWorld(Object unloadedWorld) {
        if (world == unloadedWorld) {
            clear();
        }
    }

    public static void clear() {
        if (world == null && SOURCES.isEmpty()) {
            return;
        }
        for (SourceState source : SOURCES.values()) {
            PENDING_REBUILDS.addAll(source.litSections);
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == world) {
            flushRebuilds(minecraft.levelRenderer);
        } else {
            PENDING_REBUILDS.clear();
        }
        SOURCES.clear();
        CELLS.clear();
        world = null;
    }

    public static Status status() {
        return new Status(
                ClientOptimizationConfig.dynamicLights,
                active(),
                shaderBlocked,
                SOURCES.size(),
                CELLS.size(),
                updatesLastTick,
                rebuildsLastTick,
                queries,
                candidates,
                DynamicLightResourceLoader.INSTANCE.itemDefinitionCount(),
                DynamicLightResourceLoader.INSTANCE.blockEntityDefinitionCount()
        );
    }

    private static boolean active() {
        return ClientOptimizationConfig.optimizationsEnabled()
                && ClientOptimizationConfig.dynamicLights
                && !shaderBlocked
                && !STANDALONE_DYNAMIC_LIGHTS;
    }

    private static void ensureWorld(ClientLevel level) {
        if (world != level) {
            clear();
            world = level;
        }
    }

    private static void remove(Object owner) {
        SourceState source = SOURCES.remove(owner);
        if (source == null) {
            return;
        }
        removeFromCell(source);
        PENDING_REBUILDS.addAll(source.litSections);
    }

    private static void addToCell(SourceState source) {
        source.cell = SectionPos.asLong(
                SectionPos.blockToSectionCoord(Mth.floor(source.x)),
                SectionPos.blockToSectionCoord(Mth.floor(source.y)),
                SectionPos.blockToSectionCoord(Mth.floor(source.z))
        );
        CELLS.computeIfAbsent(source.cell, ignored -> new ArrayList<>()).add(source);
    }

    private static void removeFromCell(SourceState source) {
        if (source.cell == Long.MIN_VALUE) {
            return;
        }
        ArrayList<SourceState> cell = CELLS.get(source.cell);
        if (cell != null) {
            cell.remove(source);
            if (cell.isEmpty()) {
                CELLS.remove(source.cell);
            }
        }
        source.cell = Long.MIN_VALUE;
    }

    private static LongOpenHashSet affectedSections(double x, double y, double z, int luminance) {
        LongOpenHashSet sections = new LongOpenHashSet();
        if (luminance <= 0) {
            return sections;
        }
        int minX = SectionPos.blockToSectionCoord(Mth.floor(x - MAX_RADIUS));
        int maxX = SectionPos.blockToSectionCoord(Mth.floor(x + MAX_RADIUS));
        int minY = SectionPos.blockToSectionCoord(Mth.floor(y - MAX_RADIUS));
        int maxY = SectionPos.blockToSectionCoord(Mth.floor(y + MAX_RADIUS));
        int minZ = SectionPos.blockToSectionCoord(Mth.floor(z - MAX_RADIUS));
        int maxZ = SectionPos.blockToSectionCoord(Mth.floor(z + MAX_RADIUS));
        for (int sectionX = minX; sectionX <= maxX; sectionX++) {
            for (int sectionY = minY; sectionY <= maxY; sectionY++) {
                for (int sectionZ = minZ; sectionZ <= maxZ; sectionZ++) {
                    sections.add(SectionPos.asLong(sectionX, sectionY, sectionZ));
                }
            }
        }
        return sections;
    }

    private static void flushRebuilds(LevelRenderer renderer) {
        updatesLastTick = updatesThisTick;
        updatesThisTick = 0;
        rebuildsLastTick = PENDING_REBUILDS.size();
        for (long section : PENDING_REBUILDS) {
            renderer.setSectionDirty(SectionPos.x(section), SectionPos.y(section), SectionPos.z(section));
        }
        PENDING_REBUILDS.clear();
    }

    private static int entityLuminance(Entity entity) {
        int luminance = entity.isOnFire() ? 15 : 0;
        boolean submerged = entity.isUnderWater();

        if (entity instanceof LivingEntity living) {
            for (ItemStack stack : living.getAllSlots()) {
                luminance = Math.max(luminance,
                        DynamicLightResourceLoader.INSTANCE.itemLuminance(stack, submerged));
            }
        }
        if (entity instanceof ItemEntity itemEntity) {
            luminance = Math.max(luminance,
                    DynamicLightResourceLoader.INSTANCE.itemLuminance(itemEntity.getItem(), submerged));
        }
        if (entity instanceof ItemFrame itemFrame) {
            luminance = Math.max(luminance,
                    DynamicLightResourceLoader.INSTANCE.itemLuminance(itemFrame.getItem(), submerged));
        }
        if (entity instanceof EnderMan enderMan && enderMan.getCarriedBlock() != null) {
            luminance = Math.max(luminance, enderMan.getCarriedBlock().getLightEmission());
        }
        if (entity instanceof Creeper creeper && creeper.getSwelling(0.0F) > 0.001F) {
            luminance = Math.max(luminance, 15);
        }
        if (entity instanceof PrimedTnt
                || entity instanceof AbstractHurtingProjectile
                || entity instanceof FireworkRocketEntity) {
            luminance = 15;
        } else if (entity.getType() == EntityType.BLAZE) {
            luminance = Math.max(luminance, 10);
        } else if (entity.getType() == EntityType.MAGMA_CUBE) {
            luminance = Math.max(luminance, 8);
        } else if (entity.getType() == EntityType.SPECTRAL_ARROW) {
            luminance = Math.max(luminance, 8);
        } else if (entity.getType() == EntityType.GLOW_ITEM_FRAME) {
            luminance = Math.max(luminance, 14);
        }
        return luminance;
    }

    private static final class SourceState {
        private final Object owner;
        private double x;
        private double y;
        private double z;
        private int luminance;
        private long cell = Long.MIN_VALUE;
        private long lastSeenTick;
        private long nextUpdateTick;
        private boolean initialized;
        private LongOpenHashSet litSections = new LongOpenHashSet();

        private SourceState(Object owner) {
            this.owner = owner;
        }
    }

    public record Status(
            boolean configured,
            boolean active,
            boolean shaderBlocked,
            int sources,
            int cells,
            int updates,
            int rebuilds,
            long queries,
            long candidates,
            int itemDefinitions,
            int blockEntityDefinitions
    ) {
    }
}
