package dev.hoyin1600p.vault_render_optimization.client.entitygpu;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

/**
 * A CPU fill into a buffer shorter than the batch it was reserved in (Oculus' stand-in upload of a parked
 * segment can be shorter than the original pop) skips the holes that do not fit instead of throwing.
 */
class HoleBatchFillBoundsTest {
    @Test
    void fillIntoShortBufferSkipsInsteadOfThrowing() {
        for (boolean iris : new boolean[] {false, true}) {
            GpuEntitySelfTest.ItemCase test = GpuEntitySelfTest.buildItems(11, 40, iris);
            HoleBatch batch = GpuItemTestSupport.batch(test, null, iris);
            ByteBuffer full = GpuItemTestSupport.sentinelBuffer(test);
            ByteBuffer half = full.duplicate().order(full.order());
            half.limit(full.limit() / 2);
            long before = HoleBatch.FILL_WRITES_SKIPPED.get();
            try {
                assertDoesNotThrow(() -> batch.fillOnCpu(half));
                assertTrue(HoleBatch.FILL_WRITES_SKIPPED.get() > before, "holes past the limit are counted");
            } finally {
                org.lwjgl.system.MemoryUtil.memFree(full);
                batch.reset();
            }
        }
    }
}
