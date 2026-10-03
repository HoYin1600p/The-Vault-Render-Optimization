package dev.hoyin1600p.vault_render_optimization.client.entitygpu.geckolib;

import dev.hoyin1600p.vault_render_optimization.client.entitygpu.InstanceRecord;
import dev.hoyin1600p.vault_render_optimization.client.entitygpu.ModelMesh;
import software.bernie.geckolib3.geo.render.built.GeoCube;
import software.bernie.geckolib3.geo.render.built.GeoQuad;
import software.bernie.geckolib3.geo.render.built.GeoVertex;

/**
 * Reads one GeckoLib 3 cube exactly as {@code IGeoRenderer.renderCube}/{@code createVerticesOfQuad}
 * consume it: per non-null quad, its four vertices in order with the vertex position (already in model
 * units, no division), {@code textureU}/{@code textureV} and the quad's own normal.
 */
public final class GeckoMeshCapture {
    private GeckoMeshCapture() {
    }

    /** @return the mesh, or null when a quad does not have exactly four vertices (the cube stays on the CPU) */
    public static ModelMesh capture(GeoCube cube) {
        GeoQuad[] quads = cube.quads;
        if (quads == null) return null;
        int vertices = 0;
        for (GeoQuad quad : quads) {
            if (quad == null) continue;
            if (quad.vertices == null || quad.vertices.length != 4 || quad.normal == null) return null;
            for (GeoVertex vertex : quad.vertices) {
                if (vertex == null || vertex.position == null) return null;
            }
            vertices += 4;
        }
        if (vertices == 0) return ModelMesh.EMPTY;
        float[] data = new float[vertices * ModelMesh.FLOATS_PER_VERTEX];
        int i = 0;
        for (GeoQuad quad : quads) {
            if (quad == null) continue;
            float nx = quad.normal.x(), ny = quad.normal.y(), nz = quad.normal.z();
            for (GeoVertex vertex : quad.vertices) {
                data[i++] = vertex.position.x();
                data[i++] = vertex.position.y();
                data[i++] = vertex.position.z();
                data[i++] = vertex.textureU;
                data[i++] = vertex.textureV;
                data[i++] = nx;
                data[i++] = ny;
                data[i++] = nz;
            }
        }
        return new ModelMesh(data, vertices);
    }

    /**
     * {@code renderCube}'s flat-cube fixes: a cube with zero size along Y or Z negates a negative X of the
     * transformed normal, and likewise for Y (zero X or Z) and Z (zero X or Y).
     */
    public static int flips(GeoCube cube) {
        float x = cube.size.x(), y = cube.size.y(), z = cube.size.z();
        int flips = 0;
        if (y == 0.0F || z == 0.0F) flips |= InstanceRecord.FLIP_X;
        if (x == 0.0F || z == 0.0F) flips |= InstanceRecord.FLIP_Y;
        if (x == 0.0F || y == 0.0F) flips |= InstanceRecord.FLIP_Z;
        return flips;
    }
}
