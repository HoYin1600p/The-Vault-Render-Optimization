package me.jellysquid.mods.sodium.client.render.chunk.passes;

/**
 * TEST-ONLY render-layer boundary. The production enum initializes Minecraft's
 * render types/Forge registries, which require a launched client. These five
 * tokens let unit tests exercise actual VRO/renderer buffer code without a world
 * or OpenGL context. Never included in the production JAR. Native enum layout
 * is checked separately against the input JAR by structural tests.
 */
public enum BlockRenderPass {
    SOLID(false), CUTOUT(false), CUTOUT_MIPPED(false), TRANSLUCENT(true), TRIPWIRE(true);
    public static final BlockRenderPass[] VALUES = values();
    public static final int COUNT = VALUES.length;
    private final boolean translucent;
    BlockRenderPass(boolean translucent) { this.translucent = translucent; }
    public boolean isTranslucent() { return translucent; }
}
