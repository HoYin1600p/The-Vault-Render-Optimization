package dev.hoyin1600p.vault_render_optimization.mixin;

import com.mojang.blaze3d.audio.Library;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Library.class)
public interface LibraryChannelPoolAccessor {
    @Accessor("staticChannels")
    Library.ChannelPool vro$getStaticChannels();
}
