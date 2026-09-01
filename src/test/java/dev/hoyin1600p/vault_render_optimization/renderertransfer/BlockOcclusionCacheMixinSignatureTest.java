package dev.hoyin1600p.vault_render_optimization.renderertransfer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.hoyin1600p.vault_render_optimization.mixin.backport.embeddium.occlusion.BlockOcclusionCacheMixin;
import java.lang.reflect.Method;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

class BlockOcclusionCacheMixinSignatureTest {
    @Test
    void modifyArgHandlerOnlyAcceptsTheSelectedInvocationArgument() throws ReflectiveOperationException {
        Method handler = BlockOcclusionCacheMixin.class
                .getDeclaredMethod("vro$useAdjacentPosition", BlockPos.class);

        assertEquals(BlockPos.class, handler.getReturnType());
        assertEquals(1, handler.getParameterCount());
    }
}
