package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;

public class EventStoreAdaptor<EVENT> implements EventStore<EVENT> {
    private final EventStore<EVENT> delegate;

    public EventStoreAdaptor(EventStore<EVENT> delegate) {
        this.delegate = delegate;
    }

    public EventStage<Void> flush(EventPromise<Void> promise) {
        flush((EventStageListener<Void>) promise);
        return promise;
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        delegate.flush(listener);
    }

    public EventStage<EventHolder<EVENT>> append(EVENT event,
                                                 EventPromise<EventHolder<EVENT>> promise) {
        append(event, (EventStageListener<EventHolder<EVENT>>) promise);
        return promise;
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        delegate.append(event, listener);
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        delegate.recover(recoverAtEventId, observer);
    }

}
