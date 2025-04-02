package io.stateeasy.core.impl;

import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.core.EventSourceStateDef;
import io.stateeasy.core.EventStore;

public class NoopEventStore<EVENT> implements EventStore<EVENT> {
    private long eventId = 0;

    @Override
    public void initialize(EventSourceStateDef<?, EVENT> stateDef,
                           EventStageListener<Void> listener) {
        listener.success(null);
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        listener.success(null);
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        listener.success(new EventHolder<>(eventId++, event));
    }

    @Override
    public void expire(long expireBeforeEventId, EventStageListener<Boolean> listener) {
        listener.success(true);
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        if (eventId > 0) {
            throw new RuntimeException("Already recovered");
        }
        eventId = recoverAtEventId + 1;
        observer.onComplete();
    }
}
