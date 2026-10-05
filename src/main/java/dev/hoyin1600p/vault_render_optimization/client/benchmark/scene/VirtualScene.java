package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import dev.hoyin1600p.vault_render_optimization.mixin.benchmark.ArmorStandSceneAccessor;
import dev.hoyin1600p.vault_render_optimization.mixin.benchmark.ItemEntitySceneAccessor;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.particles.SimpleParticleType;

/** Client-thread-owned entities and particles. No normal entity tick or server interaction. */
public final class VirtualScene {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final SceneIds IDS = new SceneIds();
    private static final Map<Entity, Integer> ACTORS = new IdentityHashMap<>();
    private static final List<Particle> PARTICLES = new ArrayList<>();
    private static final List<EntityType<? extends Mob>> MOB_TYPES = List.of(
            EntityType.ZOMBIE, EntityType.SKELETON, EntityType.CREEPER, EntityType.VILLAGER,
            EntityType.IRON_GOLEM, EntityType.COW, EntityType.PIG, EntityType.SHEEP,
            EntityType.SPIDER, EntityType.WOLF);
    private static final Item[] DROPS = {Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.APPLE,
            Items.STONE, Items.OAK_LOG, Items.GLASS, Items.CHEST, Items.DIAMOND_SWORD,
            Items.DIAMOND_PICKAXE, Items.NETHERITE_AXE, Items.BOW};
    // Head, chest, legs, feet.
    private static final Item[] DIAMOND_ARMOUR = {Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE,
            Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS};
    private static final Item[] NETHERITE_ARMOUR = {Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE,
            Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS};
    private static final Item[] HANDS = {Items.DIAMOND_SWORD, Items.SHIELD,
            Items.NETHERITE_PICKAXE, Items.STONE};
    private static ClientLevel level;
    private static List<SceneLayout.Position> positions = List.of();
    private static Random random;
    private static List<SceneLayout.Position> particleGaps = List.of();
    private static final SimpleParticleType[] PARTICLE_TYPES = {ParticleTypes.CRIT, ParticleTypes.ENCHANT,
            ParticleTypes.END_ROD, ParticleTypes.HAPPY_VILLAGER, ParticleTypes.SMALL_FLAME};

    private VirtualScene() { }

    public static boolean isVirtual(Entity entity) {
        return ACTORS.containsKey(entity);
    }

    /** Centre of the spawned scene at about mob mid-height, or null when no scene exists. */
    public static Vec3 centre() {
        if (positions.isEmpty()) return null;
        double x = 0, y = 0, z = 0;
        for (SceneLayout.Position p : positions) {
            x += p.x();
            y += p.y();
            z += p.z();
        }
        int n = positions.size();
        return new Vec3(x / n, y / n + 1.0, z / n);
    }

    public static void spawn(ClientLevel target, Vec3 eye, float yaw) {
        requireClientThread();
        clear();
        level = target;
        random = new Random(SceneComposition.SEED);
        try {
            List<SceneComposition.Actor> recipe = new ArrayList<>();
            List<Entity> entities = new ArrayList<>();
            List<String> skippedIds = new ArrayList<>();
            for (SceneComposition.Actor actor : SceneComposition.actors()) {
                Entity entity = create(actor, skippedIds);
                entities.add(entity);
                recipe.add(new SceneComposition.Actor(actor.kind(), actor.variant(), actor.enchanted(),
                        entity.getType().getDimensions().width));
            }
            if (!skippedIds.isEmpty()) {
                LOGGER.info("Benchmark scene used vanilla fallbacks for {} Vault mob slots; skipped ids: {}",
                        skippedIds.size(), new LinkedHashSet<>(skippedIds));
            }
            positions = SceneHeight.select(recipe, eye.x, eye.y, eye.z, yaw,
                    // Leaves are ignored: a tree canopy must not lift the whole scene into the sky.
                    (x, z) -> target.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)).positions();
            List<SceneLayout.Position> gaps = new ArrayList<>();
            for (int i = 1; i < positions.size(); i++) {
                var a = positions.get(i - 1);
                var b = positions.get(i);
                if (Math.hypot(a.x() - b.x(), a.z() - b.z()) <= Math.max(SceneLayout.ROW_SPACING,
                        Math.max(recipe.get(i - 1).width(), recipe.get(i).width()) + 0.6) + 1e-6) {
                    gaps.add(new SceneLayout.Position((a.x() + b.x()) / 2, a.y(), (a.z() + b.z()) / 2, 0));
                }
            }
            particleGaps = List.copyOf(gaps);
            for (int i = 0; i < recipe.size(); i++) {
                Entity entity = entities.get(i);
                int id;
                do { id = IDS.next(); } while (target.getEntity(id) != null);
                entity.setId(id);
                entity.setUUID(new UUID(random.nextLong(), random.nextLong()));
                entity.setSilent(true);
                entity.setInvulnerable(true);
                entity.setNoGravity(true);
                entity.noPhysics = true;
                SceneLayout.Position at = positions.get(i);
                entity.moveTo(at.x(), at.y(), at.z(), at.yaw(), 0);
                entity.setDeltaMovement(Vec3.ZERO);
                entity.setOnGround(true);
                entity.setOldPosAndRot();
                entity.tickCount = i % 80;
                if (entity instanceof LivingEntity living) {
                    living.yBodyRot = living.yBodyRotO = at.yaw();
                    living.yHeadRot = living.yHeadRotO = at.yaw();
                    living.animationPosition = i * 0.37F;
                    living.animationSpeed = living.animationSpeedOld = 0.65F;
                }
                // Mark before registration: listeners/tick guards see only our exact instances.
                ACTORS.put(entity, id);
                target.putNonPlayerEntity(id, entity);
                if (target.getEntity(id) != entity) {
                    throw new IllegalStateException("A client entity listener rejected a benchmark actor");
                }
            }
        } catch (RuntimeException | Error failure) {
            clear();
            throw failure;
        }
    }

    private static Entity create(SceneComposition.Actor actor, List<String> skippedIds) {
        int variant = actor.variant();
        if (actor.kind() == SceneComposition.Kind.MOB) {
            if (actor.vaultMob()) {
                try {
                    EntityType<?> type = ForgeRegistries.ENTITIES.getValue(new ResourceLocation(actor.vaultMobId()));
                    Entity entity = type == null ? null : type.create(level);
                    if (entity instanceof LivingEntity living) {
                        if (living instanceof Mob mob) mob.setNoAi(true);
                        return living;
                    }
                } catch (RuntimeException | Error failure) {
                    // Optional mod constructors must not prevent a complete vanilla fallback scene.
                }
                skippedIds.add(actor.vaultMobId());
            }
            Mob mob = MOB_TYPES.get(actor.vanillaVariant() % MOB_TYPES.size()).create(level);
            if (mob == null) throw new IllegalStateException("Cannot create vanilla benchmark mob");
            mob.setNoAi(true);
            if (mob.getType() == EntityType.SKELETON) {
                mob.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.BOW));
            }
            return mob;
        }
        if (actor.kind() == SceneComposition.Kind.ARMOUR_STAND) {
            ArmorStand stand = new ArmorStand(EntityType.ARMOR_STAND, level);
            ((ArmorStandSceneAccessor) stand).vro$showArms(true);
            Item[] armour = variant % 2 == 0 ? DIAMOND_ARMOUR : NETHERITE_ARMOUR;
            stand.setItemSlot(EquipmentSlot.HEAD, new ItemStack(armour[0]));
            stand.setItemSlot(EquipmentSlot.CHEST, new ItemStack(armour[1]));
            stand.setItemSlot(EquipmentSlot.LEGS, new ItemStack(armour[2]));
            stand.setItemSlot(EquipmentSlot.FEET, new ItemStack(armour[3]));
            stand.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(HANDS[variant % HANDS.length]));
            stand.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(HANDS[(variant + 1) % HANDS.length]));
            return stand;
        }
        ItemStack stack = new ItemStack(DROPS[variant % DROPS.length], 1 + variant % 16);
        if (actor.enchanted()) stack.enchant(Enchantments.UNBREAKING, 1);
        ItemEntity item = new ItemEntity(EntityType.ITEM, level);
        item.setItem(stack);
        item.setNeverPickUp();
        // Item rendering uses age and a final random bob offset, rather than just tickCount.
        ((ItemEntitySceneAccessor) item).vro$bobOffset(random.nextFloat() * (float) (Math.PI * 2));
        return item;
    }

    /** Animate rendering state only. This must never call Entity.tick or LivingEntity.aiStep. */
    public static void tick(boolean stressParticles) {
        if (level == null) return;
        requireClientThread();
        for (Entity entity : ACTORS.keySet()) {
            entity.setOldPosAndRot();
            entity.tickCount++;
            if (entity instanceof LivingEntity living) {
                living.animationSpeedOld = living.animationSpeed;
                living.animationSpeed = 0.65F;
                // 1.18.2 interpolates position by subtracting speed; no positionOld field exists.
                living.animationPosition += living.animationSpeed;
                living.yBodyRotO = living.yBodyRot;
                living.yHeadRotO = living.yHeadRot;
            }
            if (entity instanceof ItemEntity item) {
                ((ItemEntitySceneAccessor) item).vro$age(entity.tickCount);
            }
        }
        PARTICLES.removeIf(particle -> !particle.isAlive());
        if (!stressParticles || particleGaps.isEmpty()) return;
        for (int i = 0; i < SceneComposition.PARTICLES_PER_TICK; i++) {
            SceneLayout.Position at = particleGaps.get(random.nextInt(particleGaps.size()));
            Particle particle = Minecraft.getInstance().particleEngine.createParticle(
                    PARTICLE_TYPES[i % PARTICLE_TYPES.length],
                    at.x() + (random.nextDouble() - 0.5) * 0.2, at.y() + 0.25 + random.nextDouble() * 0.75,
                    at.z() + (random.nextDouble() - 0.5) * 0.2, 0, 0.015, 0);
            if (particle != null) PARTICLES.add(particle);
        }
    }

    /** Idempotent, including after failed registration and when ids have been replaced externally. */
    public static void clear() {
        if (level == null && ACTORS.isEmpty() && PARTICLES.isEmpty()) return;
        requireClientThread();
        ClientLevel oldLevel = level;
        level = null;
        Map<Entity, Integer> actors = new IdentityHashMap<>(ACTORS);
        ACTORS.clear();
        Throwable removalFailure = null;
        try {
            for (Map.Entry<Entity, Integer> actor : actors.entrySet()) {
                try {
                    if (oldLevel != null && oldLevel.getEntity(actor.getValue()) == actor.getKey()) {
                        oldLevel.removeEntity(actor.getValue(), Entity.RemovalReason.DISCARDED);
                    } else {
                        actor.getKey().setRemoved(Entity.RemovalReason.DISCARDED);
                    }
                } catch (RuntimeException | Error failure) {
                    // A removal callback must not strand the rest of the scene.
                    if (removalFailure == null) removalFailure = failure;
                    else if (removalFailure != failure) removalFailure.addSuppressed(failure);
                }
            }
        } finally {
            PARTICLES.forEach(Particle::remove);
            PARTICLES.clear();
            positions = List.of();
            particleGaps = List.of();
            random = null;
        }
        if (removalFailure instanceof RuntimeException failure) throw failure;
        if (removalFailure instanceof Error failure) throw failure;
    }

    private static void requireClientThread() {
        if (!Minecraft.getInstance().isSameThread()) {
            throw new IllegalStateException("Virtual benchmark scenes must run on the client thread");
        }
    }
}
