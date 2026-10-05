package dev.hoyin1600p.vault_render_optimization.client.benchmark.scene;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class SceneCompositionTest {
    @Test void exactCompositionAndEnchantmentRatioAreStable() {
        var actors = SceneComposition.actors();
        assertEquals(30, SceneComposition.PARTICLES_PER_TICK);
        assertEquals(300, actors.size());
        assertEquals(actors, SceneComposition.actors());
        assertEquals(180, actors.stream().filter(a -> a.kind() == SceneComposition.Kind.MOB).count());
        assertEquals(40, actors.stream().filter(a -> a.kind() == SceneComposition.Kind.ARMOUR_STAND).count());
        assertEquals(80, actors.stream().filter(a -> a.kind() == SceneComposition.Kind.ITEM).count());
        assertEquals(8, actors.stream().filter(SceneComposition.Actor::enchanted).count());
        for (var kind : SceneComposition.Kind.values()) {
            var variants = actors.stream().filter(a -> a.kind() == kind).map(SceneComposition.Actor::variant).toList();
            for (int i = 0; i < variants.size(); i++) assertEquals(i, variants.get(i));
        }
    }

    @Test void alternatingVaultSlotsCycleIdsAndKeepVanillaCycle() {
        var mobs = SceneComposition.actors().stream().filter(a -> a.kind() == SceneComposition.Kind.MOB).toList();
        assertEquals(90, mobs.stream().filter(SceneComposition.Actor::vaultMob).count());
        for (int i = 0; i < mobs.size(); i++) {
            var mob = mobs.get(i);
            assertEquals(i % 2 == 1, mob.vaultMob());
            assertEquals(i / 2, mob.vanillaVariant());
            if (mob.vaultMob()) {
                assertEquals(SceneComposition.VAULT_MOB_IDS.get((i / 2) % 18), mob.vaultMobId());
                assertEquals(1.4, mob.width());
            } else assertNull(mob.vaultMobId());
        }
    }
}
