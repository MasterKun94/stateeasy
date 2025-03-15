package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;

import java.io.Closeable;
import java.io.IOException;

/**
 * An adaptor class for {@link StateStore} that provides a layer of abstraction, allowing the
 * addition of extra functionality or modification of behavior for an underlying state store. This
 * class implements both the {@link StateStore} and {@link Closeable} interfaces, enabling it to
 * manage the lifecycle of the delegate state store.
 *
 * @param <STATE> the type of the state being managed by the state store
 */
public class StateStoreAdaptor<STATE> implements StateStore<STATE>, Closeable {
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
    public void close() throws IOException {
        if (delegate instanceof Closeable closeable) {
            closeable.close();
        }
    }
}
