package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.vertex;

import com.jozufozu.flywheel.core.vertex.BlockVertexListUnsafe;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.coderbot.iris.vertices.IrisVertexFormats;
import java.nio.ByteBuffer;

/** Input format belongs to the buffer, not to the currently active shader pipeline. */
public final class BlockModelReaders {
    private BlockModelReaders() { }

    public static VertexFormat acceptedFormat(VertexFormat actual) {
        return actual == IrisVertexFormats.TERRAIN ? DefaultVertexFormat.BLOCK : actual;
    }

    private static boolean extended(VertexFormat actual, ByteBuffer data, int count) {
        if (actual != DefaultVertexFormat.BLOCK && actual != IrisVertexFormats.TERRAIN)
            throw new IllegalArgumentException("Unsupported Flywheel block model format: " + actual);
        long required = (long) count * actual.getVertexSize();
        if (count < 0 || required > data.remaining())
            throw new IllegalArgumentException("Truncated Flywheel block model data");
        return actual == IrisVertexFormats.TERRAIN;
    }

    public static BlockVertexListUnsafe read(VertexFormat actual, ByteBuffer data, int count) {
        return extended(actual, data, count) ? new IrisBlockVertexListUnsafe(data, count)
                : new BlockVertexListUnsafe(data, count);
    }

    public static BlockVertexListUnsafe.Shaded read(VertexFormat actual, ByteBuffer data, int count, int unshaded) {
        if (unshaded < 0 || unshaded > count)
            throw new IllegalArgumentException("Invalid Flywheel shading boundary");
        return extended(actual, data, count) ? new IrisBlockVertexListUnsafe.Shaded(data, count, unshaded)
                : new BlockVertexListUnsafe.Shaded(data, count, unshaded);
    }
}
