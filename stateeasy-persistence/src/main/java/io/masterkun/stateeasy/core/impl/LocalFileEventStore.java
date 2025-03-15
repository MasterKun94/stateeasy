package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.EventSourceStateDef;
import io.masterkun.stateeasy.core.EventStore;
import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogObserver;
import io.masterkun.stateeasy.indexlogging.LogSystem;
import io.masterkun.stateeasy.indexlogging.Serializer;

public class LocalFileEventStore<EVENT> implements EventStore<EVENT> {

    private final LogFileEventStoreConfig config;
    private final Serializer<EVENT> serializer;
    private EventLogger<EVENT> logger;

    public LocalFileEventStore(LogFileEventStoreConfig config,
                               Serializer<EVENT> serializer) {
        this.config = config;
        this.serializer = serializer;
    }

    @Override
    public void initialize(EventSourceStateDef<?, EVENT> stateDef,
                           EventStageListener<Void> listener) {
        try {
            LogSystem system =
                    LocalFileLogSystemProvider.getLogSystem(config.getThreadNumPerDisk());
            String name = "event-store-" + stateDef.name();
            this.logger = system.get(config.toLogConfig(name), serializer);
            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
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
    public void expire(long expireAtEventId, EventStageListener<Boolean> listener) {
        logger.expire(expireAtEventId);
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
                    logger.executor().execute(() -> recover(nextId, observer));
                }
            }

            @Override
            public void onError(Throwable e) {
                observer.onError(e);
            }
        });
    }
}
