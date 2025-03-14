package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.EventStore;

public class NoopEventStore<EVENT> implements EventStore<EVENT> {
    private long eventId = 0;

    @Override
    public void flush(EventStageListener<Void> listener) {
        listener.success(null);
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        listener.success(new EventHolder<>(eventId++, event));
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
