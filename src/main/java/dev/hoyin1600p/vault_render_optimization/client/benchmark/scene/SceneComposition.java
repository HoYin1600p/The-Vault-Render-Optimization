package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import dev.hoyin1600p.vault_render_optimization.util.ModIds;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Pure, fixed recipe shared by scene creation and tests. Variants cycle within each actor kind. */
public final class SceneComposition {
    public static final int MOBS = 180;
    public static final int ARMOUR_STANDS = 40;
    public static final int ITEMS = 80;
    public static final int TOTAL = MOBS + ARMOUR_STANDS + ITEMS;
    // A light, fixed 600-particle/second mix, independent of benchmark switch and round.
    public static final int PARTICLES_PER_TICK = 30;
    public static final long SEED = 1600;
    public enum Kind { MOB, ARMOUR_STAND, ITEM }
    public static final List<String> VAULT_MOB_IDS = Stream.of(
            "knight", "death_knight", "death_guard", "death_inquisitor", "death_dancer", "acid_hunter",
            "acid_enforcer", "acid_fiend", "acid_pursuer", "acid_priest", "naga", "crab", "scarab",
            "skeleton_gladiator", "levishroom", "overgrown_tank", "plastic_villager_tank", "t1_plastic_horde")
            .map(path -> ModIds.THE_VAULT + ":" + path).toList();

    public record Actor(Kind kind, int variant, boolean enchanted, double width) {
        public Actor(Kind kind, int variant, boolean enchanted) {
            this(kind, variant, enchanted, defaultWidth(kind, variant));
        }

        public Actor {
            if (!Double.isFinite(width) || width <= 0) throw new IllegalArgumentException("Invalid actor width");
        }

        public boolean vaultMob() { return kind == Kind.MOB && variant % 2 == 1; }
        public int vanillaVariant() { return variant / 2; }
        public String vaultMobId() {
            return vaultMob() ? VAULT_MOB_IDS.get((variant / 2) % VAULT_MOB_IDS.size()) : null;
        }

        /** Pure tests use conservative Vault widths; spawn substitutes registered type dimensions. */
        private static double defaultWidth(Kind kind, int variant) {
            if (kind == Kind.ITEM) return 0.25;
            if (kind == Kind.ARMOUR_STAND) return 0.5;
            if (variant % 2 == 1) return 1.4;
            return switch ((variant / 2) % 10) {
                case 4, 8 -> 1.4; // Iron golem, spider.
                case 5, 6, 7 -> 0.9; // Cow, pig, sheep.
                default -> 0.6;
            };
        }
    }

    private SceneComposition() { }

    public static List<Actor> actors() {
        List<Actor> actors = new ArrayList<>(TOTAL);
        int mobs = 0, stands = 0, items = 0;
        // Interleave the three kinds so each row contains all kinds of rendering work.
        while (actors.size() < TOTAL) {
            if (mobs < MOBS) actors.add(new Actor(Kind.MOB, mobs++, false));
            if (items < ITEMS) {
                actors.add(new Actor(Kind.ITEM, items, items % 10 == 0));
                items++;
            }
            if (stands < ARMOUR_STANDS) actors.add(new Actor(Kind.ARMOUR_STAND, stands++, false));
        }
        return List.copyOf(actors);
    }
}
