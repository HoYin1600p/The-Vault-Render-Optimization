package dev.hoyin1600p.vault_render_optimization.util;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

/**
 * VRO fixes that Vault now ships itself. Each mixin steps aside when the named Vault class has the
 * marker that only the fixed builds have, so older Vault builds keep VRO's fix. Detection reads
 * bytecode, never the version string.
 */
public final class VaultUpstreamFixes {
    /** A Vault class resource and a constant-pool string that only the fixed builds contain. */
    public record Marker(String vaultClass, String text) {
        boolean presentIn(byte[] classBytes) {
            return classBytes != null
                    && new String(classBytes, StandardCharsets.ISO_8859_1).contains(text);
        }
    }

    public static final Map<String, Marker> MARKERS = Map.of(
            // Vault 22.0.0+: listeners cached in firing order, cleared on every add and release.
            "VaultEventMixin", new Marker("iskallia/vault/core/event/Event.class", "orderedListeners"),
            // Vault 7209+: learned-ability list cached and invalidated on update.
            "ClientAbilityDataMixin", new Marker("iskallia/vault/client/data/ClientAbilityData.class",
                    "learnedAbilitiesOutdated"),
            // Vault 7209+: tool model locations built once, gear read through GearDataCache.
            "ToolItemRendererMixin", new Marker("iskallia/vault/item/render/ToolItemRenderer.class", "HEAD_MODELS"),
            // Vault 7209+: armour texture cached on the render path, gear reads go through GearDataCache.
            "VaultArmorItemMixin", new Marker("iskallia/vault/item/gear/VaultArmorItem.class", "cachedArmourTexture"),
            // Vault 7209+: one armour layer factory per model, as VRO did.
            "VaultArmorRenderPropertiesMixin", new Marker(
                    "iskallia/vault/gear/renderer/VaultArmorRenderProperties.class", "layerFactoryFor"),
            // Vault 7209+: its own spawner mixin cancels renderEntity, the only caller VRO hooks.
            "ISpawnerRendererMixin", new Marker("iskallia/vault/mixin/MixinSpawnerBlockRenderer.class",
                    "cullLikeEveryOtherBlockEntity"),
            // Vault 7237+: MixinItemStack cancels setEntityRepresentation for every empty stack.
            "EmptyItemStackEntityReferenceMixin", new Marker("iskallia/vault/mixin/MixinItemStack.class",
                    "keepEmptyStacksFreeOfEntityLinks")
    );

    private VaultUpstreamFixes() {
    }

    /** Whether Vault already ships the fix for {@code mixinSimpleName}; unreadable classes mean no. */
    public static boolean absorbed(String mixinSimpleName, Function<String, byte[]> vaultClassBytes) {
        Marker marker = MARKERS.get(mixinSimpleName);
        return marker != null && marker.presentIn(vaultClassBytes.apply(marker.vaultClass()));
    }
}
