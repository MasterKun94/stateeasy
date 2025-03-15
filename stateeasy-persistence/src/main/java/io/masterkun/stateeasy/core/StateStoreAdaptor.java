package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.impl.SnapshotAndId;

public class StateStoreAdaptor<STATE> implements StateStore<STATE> {
    private final StateStore<STATE> delegate;

    public StateStoreAdaptor(StateStore<STATE> delegate) {
        this.delegate = delegate;
    }

    public EventStage<Void> initialize(StateDef<STATE, ?> stateDef, EventPromise<Void> promise) {
        initialize(stateDef, (EventStageListener<Void>) promise);
        return promise;
    }

    @Override
    public void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener) {
        delegate.initialize(stateDef, listener);
    }

    public EventStage<Long> write(Snapshot<STATE> snapshot, EventPromise<Long> promise) {
        write(snapshot, (EventStageListener<Long>) promise);
        return promise;
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Long> listener) {
        delegate.write(snapshot, listener);
    }

    public EventStage<SnapshotAndId<STATE>> read(EventPromise<SnapshotAndId<STATE>> promise) {
        read((EventStageListener<SnapshotAndId<STATE>>) promise);
        return promise;
    }

    @Override
    public void read(EventStageListener<SnapshotAndId<STATE>> listener) {
        delegate.read(listener);
    }

    public EventStage<Boolean> expire(long expireBeforeSnapshotId, EventPromise<Boolean> promise) {
        expire(expireBeforeSnapshotId, (EventStageListener<Boolean>) promise);
        return promise;
    }

    @Override
    public void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener) {
        delegate.expire(expireBeforeSnapshotId, listener);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
