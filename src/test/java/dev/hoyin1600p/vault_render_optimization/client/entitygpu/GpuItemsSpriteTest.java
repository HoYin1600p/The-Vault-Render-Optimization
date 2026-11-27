package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.mockito.Mockito.mock;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

class GpuItemsSpriteTest {
    private static BakedQuad quad(TextureAtlasSprite sprite) {
        return new BakedQuad(new int[32], -1, Direction.UP, sprite, true);
    }

    @Test void eachSpriteIsMarkedOnceInFirstUseOrder() {
        TextureAtlasSprite magma = mock(TextureAtlasSprite.class);
        TextureAtlasSprite stone = mock(TextureAtlasSprite.class);
        BakedQuad[] quads = {quad(magma), quad(stone), quad(magma), quad(magma), quad(stone)};
        assertArrayEquals(new TextureAtlasSprite[] {magma, stone}, GpuItems.distinctSprites(quads));
    }

    @Test void missingSpritesAreSkipped() {
        TextureAtlasSprite magma = mock(TextureAtlasSprite.class);
        BakedQuad[] quads = {quad(null), quad(magma), quad(null)};
        assertArrayEquals(new TextureAtlasSprite[] {magma}, GpuItems.distinctSprites(quads));
        assertArrayEquals(new TextureAtlasSprite[0], GpuItems.distinctSprites(new BakedQuad[0]));
    }
}
