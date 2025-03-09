package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.IncrementalSnapshotStateDef;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.StateDef;
import io.masterkun.stateeasy.core.StateStore;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public class MemoryStateStore<STATE> implements StateStore<STATE> {
    protected boolean initialized = false;
    protected boolean incremental = false;
    private BiFunction<STATE, STATE, STATE> stateMerger;
    private volatile Snapshot<STATE> snapshot;

    @Override
    public CompletableFuture<Void> initialize(StateDef<STATE, ?> stateDef) {
        if (initialized) {
            throw new RuntimeException("Already initialized");
        }
        initialized = true;
        if (stateDef instanceof IncrementalSnapshotStateDef<STATE,?> incStateDef) {
            incremental = true;
            stateMerger = incStateDef::merge;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Void> write(Snapshot<STATE> snapshot) {
        if (incremental && this.snapshot != null) {
            this.snapshot = new Snapshot<>(
                    snapshot.snapshotId(),
                    stateMerger.apply(this.snapshot.state(), snapshot.state()),
                    snapshot.eventId(),
                    snapshot.metadata());
        } else {
            this.snapshot = snapshot;
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<Snapshot<STATE>> read() {
        return CompletableFuture.completedFuture(snapshot);
    }

    @Override
    public void close() {

    }
}
