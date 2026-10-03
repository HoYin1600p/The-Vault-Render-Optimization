package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.iris;

import com.mojang.math.Matrix4f;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class GlUniformMcMatrix4f extends GlUniform<Matrix4f>{

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(16);

    public GlUniformMcMatrix4f(int index) {
        super(index);
    }


    public void set(Matrix4f value) {
        if (this.index < 0) return;
        value.store(buf);
        GL30C.glUniformMatrix4fv(this.index, false, buf);
    }
}
