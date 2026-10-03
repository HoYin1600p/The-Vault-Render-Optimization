package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraftforge.fml.ModList;

/**
 * Oculus' extended entity vertex format and the IDs its {@code MixinBufferBuilder} writes into every vertex
 * ({@code CapturedRenderingState}'s current entity, block entity and item). Only {@link Holder} touches Oculus
 * classes, so nothing here loads them when Oculus is absent.
 */
public final class OculusExtendedEntity {
    private static final boolean OCULUS = isLoaded();

    private OculusExtendedEntity() {
    }

    private static boolean isLoaded() {
        try {
            ModList mods = ModList.get();
            return mods != null && mods.isLoaded("oculus");
        } catch (Throwable failure) {
            return false;
        }
    }

    public static boolean installed() {
        return OCULUS;
    }

    /** {@code IrisVertexFormats.ENTITY}, or null without Oculus or when it cannot be read. */
    static VertexFormat format() {
        if (!OCULUS) return null;
        try {
            VertexFormat format = Holder.format();
            return format != null && format.getVertexSize() == IrisEntityExtension.VERTEX_BYTES ? format : null;
        } catch (Throwable failure) {
            return null;
        }
    }

    /** {@link IrisEntityExtension#idsWord} of the entity and block entity Oculus is rendering now. */
    public static int idsWord() {
        return Holder.idsWord();
    }

    /** {@link IrisEntityExtension#itemWord} of the item Oculus is rendering now. */
    public static int itemWord() {
        return Holder.itemWord();
    }

    private static final class Holder {
        static VertexFormat format() {
            return net.coderbot.iris.vertices.IrisVertexFormats.ENTITY;
        }

        static int idsWord() {
            net.coderbot.iris.uniforms.CapturedRenderingState state = net.coderbot.iris.uniforms.CapturedRenderingState.INSTANCE;
            return IrisEntityExtension.idsWord(state.getCurrentRenderedEntity(), state.getCurrentRenderedBlockEntity());
        }

        static int itemWord() {
            return IrisEntityExtension.itemWord(net.coderbot.iris.uniforms.CapturedRenderingState.INSTANCE.getCurrentRenderedItem());
        }
    }
}
