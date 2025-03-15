package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.EventSourceStateDef;
import io.masterkun.stateeasy.core.EventStore;

public class MemoryEventStore<EVENT> implements EventStore<EVENT> {
    private final EventExecutor executor;
    private final Object[] elems;
    private long writerId = 0;

    public MemoryEventStore(EventExecutor executor, int capacity) {
        this.executor = executor;
        elems = new Object[capacity];
    }

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
        if (executor.inExecutor()) {
            doAppend(event, listener);
        } else {
            executor.execute(() -> doAppend(event, listener));
        }
    }

    private void doAppend(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        int id = (int) (writerId % elems.length);
        elems[id] = event;
        listener.success(new EventHolder<>(writerId++, event));
    }

    @Override
    public void expire(long expireBeforeEventId, EventStageListener<Boolean> listener) {
        listener.success(true);
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
