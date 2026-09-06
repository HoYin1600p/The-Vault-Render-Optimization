package dev.hoyin1600p.vault_render_optimization.client.sophisticatedstorage;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class SophisticatedStorageMixinStructureTest {
    private static final Path STORAGE_UPDATE_MIXIN = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/sophisticatedstorage/StorageBlockEntityRenderUpdateMixin.java"
    );
    private static final Path BARREL_UPDATE_MIXIN = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/sophisticatedstorage/BarrelRenderInfoUpdateMixin.java"
    );
    private static final Path DYNAMIC_UPDATE_MIXIN = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/mixin/sophisticatedstorage/DynamicRenderTrackerUpdateMixin.java"
    );
    private static final Path MODEL_SNAPSHOT = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/client/sophisticatedstorage/BarrelModelSignature.java"
    );
    private static final Path FILL_RENDERER = Path.of(
            "src/main/java/dev/hoyin1600p/vault_render_optimization/client/sophisticatedstorage/LimitedBarrelFillRenderer.java"
    );

    @Test
    void defersEveryPacketNotificationUntilAllBarrelFieldsHaveLoaded() throws IOException {
        String storage = Files.readString(STORAGE_UPDATE_MIXIN);
        String barrel = Files.readString(BARREL_UPDATE_MIXIN);
        String dynamic = Files.readString(DYNAMIC_UPDATE_MIXIN);

        assertTrue(storage.contains("method = \"onDataPacket("));
        assertTrue(storage.contains("vro$beforePacketModel = BarrelModelSignature.capture(barrel)"));
        assertTrue(storage.contains("at = @At(\"RETURN\")"));
        assertTrue(barrel.contains("setChangeListener(Ljava/util/function/Consumer;)V"));
        assertTrue(dynamic.contains("method = \"onRenderInfoUpdated("));
        assertTrue(dynamic.contains("method = \"updateDynamicFlags("));
        assertTrue(barrel.contains("WorldHelper.notifyBlockUpdate(barrel)"));
    }

    @Test
    void snapshotExcludesStackCountButIncludesModelBearingState() throws IOException {
        String source = Files.readString(MODEL_SNAPSHOT);

        assertTrue(source.contains("stackTag.remove(\"Count\")"));
        assertTrue(source.contains("barrel.getMaterials()"));
        assertTrue(source.contains("barrel.shouldShowTier()"));
        assertTrue(source.contains("barrel.hasFullyDynamicRenderer()"));
    }

    @Test
    void fillFastPathDoesNotAllocateLegacyVectorVertices() throws IOException {
        String source = Files.readString(FILL_RENDERER);

        assertFalse(source.contains("new Vector4f"));
        assertTrue(source.contains("consumer.vertex(pose.pose()"));
    }
}
