package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;

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

    public EventStage<Void> write(Snapshot<STATE> snapshot, EventPromise<Void> promise) {
        write(snapshot, (EventStageListener<Void>) promise);
        return promise;
    }

    @Override
    public void write(Snapshot<STATE> snapshot, EventStageListener<Void> listener) {
        delegate.write(snapshot, listener);
    }

    public EventStage<Snapshot<STATE>> read(EventPromise<Snapshot<STATE>> promise) {
        read((EventStageListener<Snapshot<STATE>>) promise);
        return promise;
    }

    @Override
    public void read(EventStageListener<Snapshot<STATE>> listener) {
        delegate.read(listener);
    }

    @Override
    public void close() {
        delegate.close();
    }
}
