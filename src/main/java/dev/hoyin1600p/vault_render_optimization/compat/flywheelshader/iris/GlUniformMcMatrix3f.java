package dev.hoyin1600p.vault_render_optimization.compat.flywheelshader.iris;

import com.mojang.math.Matrix3f;
import me.jellysquid.mods.sodium.client.gl.shader.uniform.GlUniform;
import org.lwjgl.opengl.GL30C;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;

public class GlUniformMcMatrix3f extends GlUniform<Matrix3f>{

    private final FloatBuffer buf = BufferUtils.createFloatBuffer(9);

    public GlUniformMcMatrix3f(int index) {
        super(index);
    }


    public void set(Matrix3f value) {
        if (this.index < 0) return;
        value.store(buf);
        GL30C.glUniformMatrix3fv(this.index, false, buf);
    }
}
