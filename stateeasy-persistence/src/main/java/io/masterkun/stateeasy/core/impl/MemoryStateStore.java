package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.SnapshotAndId;
import io.masterkun.stateeasy.core.StateDef;
import io.masterkun.stateeasy.core.StateStore;

public class MemoryStateStore<STATE> implements StateStore<STATE> {
    private final EventExecutor executor;
    protected boolean initialized = false;
    private long snapshotId = 0;
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
            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Long> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> write(snapshot, listener));
            return;
        }
        try {
            this.snapshot = snapshot;
            listener.success(snapshotId++);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void read(EventStageListener<SnapshotAndId<STATE>> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> read(listener));
            return;
        }
        listener.success(snapshot == null ? null : new SnapshotAndId<>(snapshotId - 1, snapshot));
    }

    @Override
    public void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener) {
        if (!executor.inExecutor()) {
            executor.execute(() -> expire(expireBeforeSnapshotId, listener));
            return;
        }
        listener.success(false);
    }
}
