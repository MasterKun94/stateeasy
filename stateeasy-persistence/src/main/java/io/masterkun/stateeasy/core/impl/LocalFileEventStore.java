package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.EventSourceStateDef;
import io.masterkun.stateeasy.core.EventStore;
import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogObserver;
import io.masterkun.stateeasy.indexlogging.LogSystem;
import io.masterkun.stateeasy.indexlogging.Serializer;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.Closeable;
import java.io.IOException;

/**
 * Implementation of the {@link EventStore} interface that stores events in a local file system.
 * This class uses a log-based approach to manage and persist events, ensuring durability and
 * efficient retrieval. The configuration for the file system and event serialization is provided
 * through the constructor.
 *
 * @param <EVENT> the type of events that this store will manage
 */
public class LocalFileEventStore<EVENT> implements EventStore<EVENT>, Closeable {

    private final LogFileEventStoreConfig config;
    private final Serializer<EVENT> serializer;
    @VisibleForTesting
    EventLogger<EVENT> logger;

    public LocalFileEventStore(LogFileEventStoreConfig config,
                               Serializer<EVENT> serializer) {
        this.config = config;
        this.serializer = serializer;
    }

    @Override
    public void initialize(EventSourceStateDef<?, EVENT> stateDef,
                           EventStageListener<Void> listener) {
        try {
            LogSystem system = LocalFileLogSystemProvider
                    .getLogSystem(config.getThreadNumPerDisk());
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
    public void expire(long expireBeforeEventId, EventStageListener<Boolean> listener) {
        logger.expire(expireBeforeEventId, listener);
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

    @Override
    public void close() throws IOException {
        LocalFileLogSystemProvider.getLogSystem(1)
                .closeLogger(logger);
        logger = null;
    }
}
