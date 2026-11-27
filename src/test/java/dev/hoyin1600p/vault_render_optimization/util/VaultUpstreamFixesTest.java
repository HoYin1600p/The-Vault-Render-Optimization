package dev.hoyin1600p.vault_render_optimization.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class VaultUpstreamFixesTest {
    @Test
    void coversEveryFixVaultShipsItself() {
        assertEquals(Set.of("VaultEventMixin", "ClientAbilityDataMixin", "ToolItemRendererMixin",
                "VaultArmorItemMixin", "VaultArmorRenderPropertiesMixin", "ISpawnerRendererMixin",
                "EmptyItemStackEntityReferenceMixin"),
                VaultUpstreamFixes.MARKERS.keySet());
    }

    @Test
    void onlyAClassWithTheMarkerCountsAsAbsorbed() {
        String event = "iskallia/vault/core/event/Event.class";
        String itemStack = "iskallia/vault/mixin/MixinItemStack.class";
        Map<String, byte[]> fixed = Map.of(
                event, bytes("\u0000orderedListeners\u0000Ljava/util/List;"),
                itemStack, bytes("\u0000keepEmptyStacksFreeOfEntityLinks\u0000setEntityRepresentation")
        );
        Map<String, byte[]> old = Map.of(
                event, bytes("\u0000listeners\u0000getListeners"),
                itemStack, bytes("\u0000useGearRarity\u0000getGearHoverName")
        );
        assertTrue(VaultUpstreamFixes.absorbed("VaultEventMixin", fixed::get));
        assertFalse(VaultUpstreamFixes.absorbed("VaultEventMixin", old::get));
        assertTrue(VaultUpstreamFixes.absorbed("EmptyItemStackEntityReferenceMixin", fixed::get));
        assertFalse(VaultUpstreamFixes.absorbed("EmptyItemStackEntityReferenceMixin", old::get));
        assertFalse(VaultUpstreamFixes.absorbed("VaultEventMixin", name -> null));
        assertFalse(VaultUpstreamFixes.absorbed("ClientAbilityDataMixin", fixed::get));
        assertFalse(VaultUpstreamFixes.absorbed("NotAVaultFix", fixed::get));
    }

    private static byte[] bytes(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }
}
