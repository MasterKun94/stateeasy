package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.core.impl.MemoryStateStore;

import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestStateStore<STATE> extends MemoryStateStore<STATE> {
    private final Queue<Snapshot<STATE>> queue = new ConcurrentLinkedQueue<>();
    private final StateStore<STATE> internal;
    private StateDef<?, ?> stateDef;

    public TestStateStore(StateStore<STATE> internal) {
        this.internal = internal;
    }

    public TestStateStore() {
        this.internal = null;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isIncremental() {
        return incremental;
    }

    @Override
    public CompletableFuture<Void> initialize(StateDef<STATE, ?> stateDef) {
        if (initialized && this.stateDef == stateDef) {
            return CompletableFuture.completedFuture(null);
        }
        this.stateDef = stateDef;
        var future = super.initialize(stateDef);
        if (internal != null) {
            future = future.thenCompose(v -> internal.initialize(stateDef));
        }
        return future;
    }

    @Override
    public CompletableFuture<Void> write(Snapshot<STATE> snapshot) {
        queue.add(snapshot);
        var future = super.write(snapshot);
        if (internal != null) {
            future = future.thenCompose(v -> internal.write(snapshot));
        }
        return future;
    }

    @Override
    public CompletableFuture<Snapshot<STATE>> read() {
        var future = super.read();
        if (internal != null) {
            future = future.thenCompose(snapshot ->
                    internal.read().thenApply(snapshot1 -> {
                        if (!snapshot.equals(snapshot1)) {
                            throw new RuntimeException("snapshot not equal");
                        }
                        return snapshot1;
                    }));
        }
        return future;
    }
}
