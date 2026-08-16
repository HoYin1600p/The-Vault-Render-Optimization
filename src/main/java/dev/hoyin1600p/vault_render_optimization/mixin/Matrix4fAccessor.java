package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.math.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Matrix4f.class)
public interface Matrix4fAccessor {
    @Accessor("m00") float vro$m00();
    @Accessor("m01") float vro$m01();
    @Accessor("m02") float vro$m02();
    @Accessor("m03") float vro$m03();
    @Accessor("m10") float vro$m10();
    @Accessor("m11") float vro$m11();
    @Accessor("m12") float vro$m12();
    @Accessor("m13") float vro$m13();
    @Accessor("m20") float vro$m20();
    @Accessor("m21") float vro$m21();
    @Accessor("m22") float vro$m22();
    @Accessor("m23") float vro$m23();
}
