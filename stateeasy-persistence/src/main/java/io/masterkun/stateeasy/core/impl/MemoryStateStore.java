package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.IncrementalSnapshotStateDef;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.StateDef;
import io.masterkun.stateeasy.core.StateStore;

import java.util.function.BiFunction;

public class MemoryStateStore<STATE> implements StateStore<STATE> {
    private final EventExecutor executor;
    protected boolean initialized = false;
    protected boolean incremental = false;
    private BiFunction<STATE, STATE, STATE> stateMerger;
    private volatile Snapshot<STATE> snapshot;

    public MemoryStateStore(EventExecutor executor) {
        this.executor = executor;
    }

    @Override
    public void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> initialize(stateDef, listener));
            return;
        }
        try {
            if (initialized) {
                throw new RuntimeException("Already initialized");
            }
            initialized = true;
            if (stateDef instanceof IncrementalSnapshotStateDef<STATE, ?> incStateDef) {
                incremental = true;
                stateMerger = incStateDef::merge;
            }
            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Void> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> write(snapshot, listener));
            return;
        }
        try {
            if (incremental && this.snapshot != null) {
                this.snapshot = new Snapshot<>(
                        snapshot.snapshotId(),
                        stateMerger.apply(this.snapshot.state(), snapshot.state()),
                        snapshot.eventId(),
                        snapshot.metadata());
            } else {
                this.snapshot = snapshot;
            }
            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void read(EventStageListener<Snapshot<STATE>> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> read(listener));
            return;
        }
        listener.success(snapshot);
    }

    @Override
    public void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> expire(expireBeforeSnapshotId, listener));
            return;
        }
        listener.success(false);
    }

    @Override
    public void close() {

    }
}
