package dev.hoyin1600p.vault_render_optimization.client.chunk.budget;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;
import java.util.function.ToLongFunction;

/** Single native consumer; producers only append. Never removes a result until next() transfers ownership. */
public final class BudgetedDrain<T> implements Iterator<T> {
    private final Queue<T> queue;
    private final ToLongFunction<T> size;
    private final Consumer<T> removed;
    private final long allowance;
    private final int maxCount;
    private long bytes;
    private int count;

    public BudgetedDrain(Queue<T> queue, ToLongFunction<T> size, Consumer<T> removed, long allowance, int maxCount) {
        this.queue = queue;
        this.size = size;
        this.removed = removed;
        this.allowance = allowance;
        this.maxCount = Math.min(AdaptiveChunkBudget.MAX_RESULTS, maxCount);
    }

    @Override public boolean hasNext() {
        T head = queue.peek();
        // An indivisible oversized result must still progress. Everything else remains in the native queue.
        return head != null && count < maxCount && (count == 0 || size.applyAsLong(head) <= allowance - bytes);
    }

    @Override public T next() {
        if (!hasNext()) throw new NoSuchElementException();
        T result = queue.remove();
        bytes += size.applyAsLong(result);
        count++;
        removed.accept(result);
        return result;
    }

    public long bytes() { return bytes; }
    public int count() { return count; }
}
