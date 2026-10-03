/*
 * Based on ImmediatelyFast Reforged's ImmediatelyFastMixinPlugin (LGPL-3.0-or-later),
 * Copyright (C) 2023 RK_01/RaphiMC and contributors. Modified by HoYin1600p for VRO, 2026:
 * every mixin is skipped unless VRO's built-in ImmediatelyFast is active (enabled, standalone
 * ImmediatelyFast absent, Oculus members present); per-feature switches are unchanged.
 */
package dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.injection;

import dev.hoyin1600p.vault_render_optimization.compat.immediatelyfast.VroImmediatelyFast;
import java.util.List;
import java.util.Set;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public class VroImmediatelyFastMixinPlugin implements IMixinConfigPlugin {
    private String mixinPackage;

    @Override
    public void onLoad(String mixinPackage) {
        this.mixinPackage = mixinPackage + ".";
        VroImmediatelyFast.applied();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!mixinClassName.startsWith(this.mixinPackage) || !VroImmediatelyFast.applied()) return false;

        final String mixinName = mixinClassName.substring(this.mixinPackage.length());
        final String packageName = mixinName.substring(0, mixinName.lastIndexOf('.'));

        if (!VroImmediatelyFast.config.font_atlas_resizing && packageName.startsWith("font_atlas_resizing")) {
            return false;
        }
        if (!VroImmediatelyFast.config.map_atlas_generation && packageName.startsWith("map_atlas_generation")) {
            return false;
        }
        if (!VroImmediatelyFast.config.hud_batching && packageName.startsWith("hud_batching")) {
            return false;
        }
        if (!VroImmediatelyFast.config.fast_text_lookup && packageName.startsWith("fast_text_lookup")) {
            return false;
        }
        return VroImmediatelyFast.config.fast_buffer_upload || !packageName.startsWith("fast_buffer_upload");
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}
