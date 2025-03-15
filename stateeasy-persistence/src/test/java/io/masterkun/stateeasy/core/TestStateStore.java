package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.impl.MemoryStateStore;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestStateStore<STATE> implements StateStore<STATE> {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();
    private final Queue<Snapshot<STATE>> queue = new ConcurrentLinkedQueue<>();
    private final StateStoreAdaptor<STATE> internal;
    private final StateStoreAdaptor<STATE> memory;
    private StateDef<?, ?> stateDef;

    public TestStateStore(StateStore<STATE> internal) {
        memory = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        this.internal = new StateStoreAdaptor<>(internal);
    }

    public TestStateStore() {
        memory = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        this.internal = null;
    }

    @Override
    public void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener) {
        if (this.stateDef == stateDef) {
            listener.success(null);
            return;
        }
        this.stateDef = stateDef;
        EventStage<Void> future = memory.initialize(stateDef, executor.newPromise());
        if (internal != null) {
            future = future.flatmap(v -> internal.initialize(stateDef, executor.newPromise()));
        }
        future.addListener(listener);
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Void> listener) {
        queue.add(snapshot);
        EventStage<Void> future = memory.write(snapshot, executor.newPromise());
        if (internal != null) {
            future = future.flatmap(v -> internal.write(snapshot, executor.newPromise()));
        }
        future.addListener(listener);
    }

    @Override
    public void read(EventStageListener<Snapshot<STATE>> listener) {
        EventStage<Snapshot<STATE>> future = memory.read(executor.newPromise());
        if (internal != null) {
            future = future.flatmap(snapshot ->
                    internal.read(executor.newPromise()).map(snapshot1 -> {
                        if (!snapshot.equals(snapshot1)) {
                            throw new RuntimeException("snapshot not equal");
                        }
                        return snapshot1;
                    }));
        }
        future.addListener(listener);
    }

    @Override
    public void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener) {
        var future = memory.expire(expireBeforeSnapshotId, executor.newPromise());
        if (internal != null) {
            future = future
                    .flatmap(b -> internal.expire(expireBeforeSnapshotId, executor.newPromise()));
        }
        future.addListener(listener);
    }

    @Override
    public void close() {
        memory.close();
        if (internal != null) {
            internal.close();
        }
    }
}
