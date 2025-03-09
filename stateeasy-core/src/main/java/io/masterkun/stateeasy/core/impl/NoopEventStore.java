package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.EventStore;

import java.util.concurrent.CompletableFuture;

public class NoopEventStore<EVENT> implements EventStore<EVENT> {
    private long eventId = 0;

    @Override
    public CompletableFuture<Void> flush() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EventHolder<EVENT>> append(EVENT event) {
        return CompletableFuture.completedFuture(new EventHolder<>(eventId++, event));
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
