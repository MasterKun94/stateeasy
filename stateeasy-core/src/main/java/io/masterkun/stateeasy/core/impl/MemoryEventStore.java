package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.EventStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MemoryEventStore<EVENT> implements EventStore<EVENT> {
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Object[] elems;
    private long writerId = 0;

    public MemoryEventStore(int capacity) {
        elems = new Object[capacity];
    }

    @Override
    public CompletableFuture<Void> flush() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<EventHolder<EVENT>> append(EVENT event) {
        return CompletableFuture.supplyAsync(() -> {
            int id = (int) (writerId % elems.length);
            elems[id] = event;
            return new EventHolder<>(writerId++, event);
        }, executor);
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        executor.execute(() -> {
            int len = elems.length;
            long minReaderId = writerId - len;
            if (minReaderId > recoverAtEventId) {
                observer.onError(new RuntimeException("id expired"));
            }
            for (long i = recoverAtEventId; i < writerId; i++) {
                int id = (int) (i % len);
                //noinspection unchecked
                observer.onEvent(new EventHolder<>(id, (EVENT) elems[id]));
            }
            observer.onComplete();
        });
    }
}
