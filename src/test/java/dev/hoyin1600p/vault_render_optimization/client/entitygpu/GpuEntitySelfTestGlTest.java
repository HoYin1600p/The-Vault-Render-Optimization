package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11C;

/**
 * Runs the real compute shader on this machine's GPU through a hidden window with the same context
 * hints Minecraft uses (3.2 core, forward compatible). Skipped where no GPU context can be created.
 */
class GpuEntitySelfTestGlTest {
    @Test
    void computeShaderMatchesReferenceBitForBit() {
        assumeTrue(GLFW.glfwInit(), "GLFW is unavailable");
        long window = 0L;
        try {
            GLFW.glfwDefaultWindowHints();
            GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
            GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_OPENGL_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_CREATION_API, GLFW.GLFW_NATIVE_CONTEXT_API);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);
            GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE);
            window = GLFW.glfwCreateWindow(64, 64, "vro-gpu-entity-self-test", 0L, 0L);
            assumeTrue(window != 0L, "no OpenGL context");
            GLFW.glfwMakeContextCurrent(window);
            GL.createCapabilities();

            GpuEntityCapabilities.Snapshot snapshot = GpuEntityCapabilities.read();
            String blocker = GpuEntityCapabilities.blocker(snapshot);
            System.out.println("GPU entity self-test on " + snapshot.renderer() + " / " + snapshot.version()
                    + " / GLSL " + snapshot.glslVersion() + " / route " + GpuEntityCapabilities.route(snapshot) + ": " + (blocker == null ? "supported" : blocker));
            assumeTrue(blocker == null, blocker);

            GpuEntityBackend backend = GpuEntityBackend.create(GpuEntityCapabilities.route(snapshot),
                    GpuEntityCapabilities.ARENA_MAX_BYTES);
            try {
                for (long seed = 1; seed <= 4; seed++) {
                    assertNull(GpuEntitySelfTest.run(backend, seed * 1600, 1500), "seed " + seed);
                }
                assertNull(GpuEntitySelfTest.run(backend, 42, 1), "single instance");
                assertNull(GpuEntitySelfTest.run(backend, 43, 70000 / 50), "many instances");
                // Enough consecutive uploads to wrap (and orphan) the 4 MiB instance ring several times.
                for (int round = 0; round < 120; round++) {
                    assertNull(GpuEntitySelfTest.run(backend, 5000 + round, 300), "ring round " + round);
                }
                // The arena path: meshes uploaded through upload() and expanded through dispatch().
                assertNull(GpuEntityModelsGlHarness.arenaRoundTrip(backend, 77), "arena round trip");
                for (long seed = 1; seed <= 3; seed++) {
                    assertNull(GpuEntityModelsGlHarness.gapUploadRoundTrip(backend, 900 + seed), "gap upload " + seed);
                }
                // Particle quads: their own program, the same ring.
                assertTrue(backend.particlesAvailable(), "particle program: " + backend.particleFailure());
                for (long seed = 1; seed <= 4; seed++) {
                    assertNull(GpuParticleSelfTest.run(backend, seed * 77, 5000), "particles seed " + seed);
                }
                assertNull(GpuParticleSelfTest.run(backend, 11, 1), "single particle");
                assertNull(GpuModelsParticleGap.roundTrip(backend, 12), "particle gap upload");
                // Oculus extended entity vertices: face normal, tangent and mid UV with Java's rounding.
                assertTrue(backend.irisAvailable(), "Oculus program: " + backend.irisFailure());
                for (long seed = 1; seed <= 8; seed++) {
                    assertNull(GpuEntitySelfTest.run(backend, seed * 311, 1500, true), "Oculus seed " + seed);
                }
                assertNull(GpuEntitySelfTest.run(backend, 44, 1, true), "Oculus single instance");
                for (long seed = 1; seed <= 16; seed++) {
                    assertNull(IrisMathGl.run(GpuEntityCapabilities.route(snapshot), seed, 1 << 20), "Oculus math seed " + seed);
                }
                // Item quads: baked quads expanded on the GPU (FORGE and EMBEDDIUM records mixed), plain and Oculus.
                assertTrue(backend.itemsAvailable(), "item program: " + backend.itemFailure());
                assertTrue(backend.itemsIrisAvailable(), "item Oculus program: " + backend.itemIrisFailure());
                for (long seed = 1; seed <= 6; seed++) {
                    assertNull(GpuEntitySelfTest.run(backend, seed * 131, 800, true, false), "item seed " + seed);
                    assertNull(GpuEntitySelfTest.run(backend, seed * 137, 800, true, true), "item Oculus seed " + seed);
                }
                assertNull(GpuEntitySelfTest.run(backend, 45, 1, true, false), "item single instance");
                assertNull(GpuEntitySelfTest.run(backend, 46, 1, true, true), "item Oculus single instance");
                for (long seed = 1; seed <= 3; seed++) {
                    assertNull(GpuItemTestSupport.arenaRoundTrip(backend, 300 + seed, 400, false), "item arena " + seed);
                    assertNull(GpuItemTestSupport.arenaRoundTrip(backend, 400 + seed, 400, true), "item Oculus arena " + seed);
                }
                assertNull(GpuEntityModelsGlHarness.glError(), "GL error");
            } finally {
                backend.close();
            }
            System.out.println("GPU entity self-test: PASS (" + GL11C.glGetString(GL11C.GL_RENDERER) + ")");
        } finally {
            if (window != 0L) {
                GLFW.glfwMakeContextCurrent(0L);
                GLFW.glfwDestroyWindow(window);
            }
            GLFW.glfwTerminate();
        }
    }
}
