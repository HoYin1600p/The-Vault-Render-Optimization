package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class BudgetedDrainTest {
    @Test void peekNeverTakesOwnershipAndRemainderStaysInNativeQueue() {
        var queue = new ArrayDeque<>(List.of(6L, 4L, 8L));
        var removed = new ArrayList<Long>();
        var drain = new BudgetedDrain<>(queue, Long::longValue, removed::add, 10, 3);
        assertTrue(drain.hasNext());
        assertTrue(drain.hasNext());
        assertEquals(3, queue.size());
        assertEquals(6, drain.next());
        assertEquals(4, drain.next());
        assertFalse(drain.hasNext());
        assertThrows(NoSuchElementException.class, drain::next);
        assertEquals(List.of(6L, 4L), removed);
        assertEquals(List.of(8L), List.copyOf(queue));
        assertEquals(10, drain.bytes());
    }

    @Test void oversizeResultProgressesWithoutSkippingOrFreeingOtherResults() {
        var queue = new ArrayDeque<>(List.of(1000L, 2L));
        var drain = new BudgetedDrain<>(queue, Long::longValue, ignored -> {}, 10, 2);
        assertEquals(1000, drain.next());
        assertFalse(drain.hasNext());
        assertEquals(2, queue.peek());
    }

    @Test void continuouslyAppendingProducersCannotExtendTheBatch() {
        var queue = new ArrayDeque<>(List.of(0L));
        var drain = new BudgetedDrain<>(queue, Long::longValue, ignored -> queue.add(0L), 10, 10000);
        while (drain.hasNext()) drain.next();
        assertEquals(128, drain.count());
        assertEquals(1, queue.size());
    }

    @Test void stoppedDrainDoesNotRetainOrDisposeResultsAndNativeDrainCanResume() {
        var queue = new ArrayDeque<>(List.of(10L, 20L, 30L));
        var drain = new BudgetedDrain<>(queue, Long::longValue, ignored -> {}, 10, 3);
        assertEquals(10, drain.next());
        assertFalse(drain.hasNext());
        assertEquals(List.of(20L, 30L), List.copyOf(queue));
        queue.clear(); // Represents native off/unload ownership, not VRO freeing data.
        assertFalse(drain.hasNext());
    }
}
