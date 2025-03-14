package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.EventStore;
import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogObserver;

public class LocalLogEventStore<EVENT> implements EventStore<EVENT> {
    private final EventExecutor executor;
    private final EventLogger<EVENT> logger;

    public LocalLogEventStore(EventExecutor executor, EventLogger<EVENT> logger) {
        this.executor = executor;
        this.logger = logger;
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        logger.flush(listener);
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        logger.write(event, new EventStageListener<>() {
            @Override
            public void success(IdAndOffset value) {
                listener.success(new EventHolder<>(value.id(), event));
            }

            @Override
            public void failure(Throwable cause) {
                listener.failure(cause);
            }
        });
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        logger.read(recoverAtEventId, 100, new LogObserver<>() {
            @Override
            public void onNext(long id, long offset, EVENT value) {
                observer.onEvent(new EventHolder<>(id, value));
            }

            @Override
            public void onComplete(long nextId, long nextOffset) {
                if (recoverAtEventId == nextId) {
                    observer.onComplete();
                } else {
                    executor.execute(() -> recover(nextId, observer));
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }
        });
    }
}
