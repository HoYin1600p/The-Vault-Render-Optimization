package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import net.minecraft.client.model.geom.ModelPart;

/** Reads a part's cubes exactly as {@code ModelPart.Cube.compile} would consume them. */
public final class ModelMeshCapture {
    private ModelMeshCapture() {
    }

    /** @return the mesh, or null when a polygon is not a quad (the part then stays on the CPU path) */
    public static ModelMesh capture(ModelPart part) {
        int vertices = 0;
        for (ModelPart.Cube cube : part.cubes) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                if (polygon.vertices.length != 4) return null;
                vertices += 4;
            }
        }
        if (vertices == 0) return ModelMesh.EMPTY;
        float[] data = new float[vertices * ModelMesh.FLOATS_PER_VERTEX];
        int i = 0;
        for (ModelPart.Cube cube : part.cubes) {
            for (ModelPart.Polygon polygon : cube.polygons) {
                float nx = polygon.normal.x(), ny = polygon.normal.y(), nz = polygon.normal.z();
                for (ModelPart.Vertex vertex : polygon.vertices) {
                    // Cube.compile divides by 16 per frame; the quotient is the same float every time.
                    data[i++] = vertex.pos.x() / 16.0F;
                    data[i++] = vertex.pos.y() / 16.0F;
                    data[i++] = vertex.pos.z() / 16.0F;
                    data[i++] = vertex.u;
                    data[i++] = vertex.v;
                    data[i++] = nx;
                    data[i++] = ny;
                    data[i++] = nz;
                }
            }
        }
        return new ModelMesh(data, vertices);
    }
}
