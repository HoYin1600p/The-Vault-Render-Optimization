/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Adapted for The Vault Render Optimization from Flerovium.
 * Upstream repository: https://github.com/MoePus/Flerovium
 * Upstream source: src/main/java/com/moepus/flerovium/mixins/Particle/SingleQuadParticleMixin.java
 * Upstream commit: 240f08c62745d57bf200440c9932e0c7907bc5f7
 * Original author: MoePus and Flerovium contributors
 * VRO modifications: isolated the camera-basis calculation into a reusable,
 * allocation-free 1.18.2 geometry helper with renderer-neutral output.
 * Modified: 2026-09-01
 */
package dev.hoyin1600p.vault_render_optimization.client.particle;

import net.minecraft.util.Mth;

public final class ParticleBillboardGeometry {
    private static final ThreadLocal<ParticleBillboardGeometry> LOCAL =
            ThreadLocal.withInitial(ParticleBillboardGeometry::new);

    private float x0;
    private float y0;
    private float z0;
    private float x1;
    private float y1;
    private float z1;
    private float x2;
    private float y2;
    private float z2;
    private float x3;
    private float y3;
    private float z3;

    private ParticleBillboardGeometry() {
    }

    public static ParticleBillboardGeometry compute(
            float leftX,
            float leftY,
            float leftZ,
            float upX,
            float upY,
            float upZ,
            float roll,
            float size,
            float positionX,
            float positionY,
            float positionZ
    ) {
        ParticleBillboardGeometry geometry = LOCAL.get();
        if (roll != 0.0F) {
            float sin = Mth.sin(roll);
            float cos = Mth.cos(roll);

            float rotatedLeftX = Math.fma(cos, leftX, sin * upX);
            float rotatedLeftY = Math.fma(cos, leftY, sin * upY);
            float rotatedLeftZ = Math.fma(cos, leftZ, sin * upZ);
            float rotatedUpX = Math.fma(-sin, leftX, cos * upX);
            float rotatedUpY = Math.fma(-sin, leftY, cos * upY);
            float rotatedUpZ = Math.fma(-sin, leftZ, cos * upZ);

            leftX = rotatedLeftX;
            leftY = rotatedLeftY;
            leftZ = rotatedLeftZ;
            upX = rotatedUpX;
            upY = rotatedUpY;
            upZ = rotatedUpZ;
        }

        geometry.x0 = Math.fma(leftX + upX, -size, positionX);
        geometry.y0 = Math.fma(leftY + upY, -size, positionY);
        geometry.z0 = Math.fma(leftZ + upZ, -size, positionZ);

        geometry.x1 = Math.fma(-leftX + upX, size, positionX);
        geometry.y1 = Math.fma(-leftY + upY, size, positionY);
        geometry.z1 = Math.fma(-leftZ + upZ, size, positionZ);

        geometry.x2 = Math.fma(leftX + upX, size, positionX);
        geometry.y2 = Math.fma(leftY + upY, size, positionY);
        geometry.z2 = Math.fma(leftZ + upZ, size, positionZ);

        geometry.x3 = Math.fma(-leftX + upX, -size, positionX);
        geometry.y3 = Math.fma(-leftY + upY, -size, positionY);
        geometry.z3 = Math.fma(-leftZ + upZ, -size, positionZ);
        return geometry;
    }

    public float x0() { return x0; }
    public float y0() { return y0; }
    public float z0() { return z0; }
    public float x1() { return x1; }
    public float y1() { return y1; }
    public float z1() { return z1; }
    public float x2() { return x2; }
    public float y2() { return y2; }
    public float z2() { return z2; }
    public float x3() { return x3; }
    public float y3() { return y3; }
    public float z3() { return z3; }
}
