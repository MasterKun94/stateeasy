package io.masterkun.stateeasy.indexlogging;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventFuture;
import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for logging events with support for asynchronous writing and reading of log entries.
 * The implementation of this interface is responsible for managing the persistence, retrieval, and
 * lifecycle of log entries.
 *
 * @param <T> the type of the objects to be logged
 */
public interface EventLogger<T> {
    /**
     * Returns the ID of the first log entry in the log.
     *
     * @return the ID of the first log entry
     */
    long startId();

    /**
     * Returns the ID of the last log entry in the log.
     *
     * @return the ID of the last log entry
     */
    long endId();

    /**
     * Returns the next available ID for a new log entry.
     *
     * @return the next ID that can be used for a new log entry
     */
    long nextId();

    /**
     * Asynchronously writes the given object to the log.
     *
     * @param obj the object to be written to the log
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID
     * and offset of the written log entry once the write operation is complete
     */
    default CompletableFuture<IdAndOffset> write(T obj) {
        return write(obj, false);
    }

    /**
     * Asynchronously writes the given object to the log and notifies the provided listener upon
     * completion or error.
     *
     * @param obj      the object to be written to the log
     * @param listener the listener to be notified with the ID and offset of the written log entry,
     *                 or any errors
     */
    default void write(T obj, EventStageListener<IdAndOffset> listener) {
        write(obj, false, listener);
    }

    /**
     * Asynchronously writes the given object to the log with an optional promise.
     *
     * @param obj     the object to be written to the log
     * @param promise the optional promise to be completed with the ID and offset of the written log
     *                entry, or any errors; if null, a new promise is created
     * @return the promise that will be completed with the ID and offset of the written log entry
     * once the write operation is complete
     */
    default EventStage<IdAndOffset> write(T obj, @Nullable EventPromise<IdAndOffset> promise) {
        return write(obj, false, promise);
    }

    /**
     * Asynchronously writes the given object to the log with an option to flush.
     *
     * @param obj   the object to be written to the log
     * @param flush if true, the write operation will be followed by a flush to ensure data is
     *              persisted
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID
     * and offset of the written log entry once the write operation is complete
     */
    default CompletableFuture<IdAndOffset> write(T obj, boolean flush) {
        return write(obj, flush, false);
    }

    /**
     * Asynchronously writes the given object to the log with an option to flush and a listener for
     * completion or error.
     *
     * @param obj      the object to be written to the log
     * @param flush    if true, the write operation will be followed by a flush to ensure data is
     *                 persisted
     * @param listener the listener to be notified with the ID and offset of the written log entry,
     *                 or any errors
     */
    default void write(T obj, boolean flush, EventStageListener<IdAndOffset> listener) {
        write(obj, flush, false, listener);
    }

    /**
     * Asynchronously writes the given object to the log with an option to flush and an optional
     * promise.
     *
     * @param obj     the object to be written to the log
     * @param flush   if true, the write operation will be followed by a flush to ensure data is
     *                persisted
     * @param promise the optional promise to be completed with the ID and offset of the written log
     *                entry, or any errors; if null, a new promise is created
     * @return the promise that will be completed with the ID and offset of the written log entry
     * once the write operation is complete
     */
    default EventStage<IdAndOffset> write(T obj, boolean flush,
                                          @Nullable EventPromise<IdAndOffset> promise) {
        return write(obj, flush, false, promise);
    }

    /**
     * Asynchronously writes the given object to the log with options for flushing and immediate
     * callback.
     *
     * @param obj               the object to be written to the log
     * @param flush             if true, the write operation will be followed by a flush to ensure
     *                          data is persisted
     * @param immediateCallback if true, the callback will be invoked immediately after the write
     *                          operation, otherwise, it will be invoked after the data is
     *                          persisted
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID
     * and offset of the written log entry once the write operation is complete
     */
    default CompletableFuture<IdAndOffset> write(T obj, boolean flush, boolean immediateCallback) {
        CompletableFuture<IdAndOffset> future = new CompletableFuture<>();
        write(obj, flush, immediateCallback, new EventStageListener<IdAndOffset>() {
            @Override
            public void success(IdAndOffset value) {
                future.complete(value);
            }

            @Override
            public void failure(Throwable cause) {
                future.completeExceptionally(cause);
            }
        });
        return future;
    }

    /**
     * Asynchronously writes the given object to the log with options for flushing, immediate
     * callback, and a listener for completion or error.
     *
     * @param obj               the object to be written to the log
     * @param flush             if true, the write operation will be followed by a flush to ensure
     *                          data is persisted
     * @param immediateCallback if true, the callback will be invoked immediately after the write
     *                          operation, otherwise, it will be invoked after the data is
     *                          persisted
     * @param listener          the listener to be notified with the ID and offset of the written
     *                          log entry, or any errors
     */
    void write(T obj, boolean flush, boolean immediateCallback,
               EventStageListener<IdAndOffset> listener);

    /**
     * Asynchronously writes the given object to the log with options for flushing, immediate
     * callback, and an optional promise.
     *
     * @param obj               the object to be written to the log
     * @param flush             if true, the write operation will be followed by a flush to ensure
     *                          data is persisted
     * @param immediateCallback if true, the callback will be invoked immediately after the write
     *                          operation, otherwise, it will be invoked after the data is
     *                          persisted
     * @param promise           the optional promise to be completed with the ID and offset of the
     *                          written log entry, or any errors; if null, a new promise is created
     * @return the promise that will be completed with the ID and offset of the written log entry
     * once the write operation is complete
     */
    default EventStage<IdAndOffset> write(T obj, boolean flush, boolean immediateCallback,
                                          @Nullable EventPromise<IdAndOffset> promise) {
        if (promise == null) {
            promise = EventPromise.newPromise(executor());
        }
        write(obj, flush, immediateCallback, (EventStageListener<IdAndOffset>) promise);
        return promise;
    }

    default CompletableFuture<Void> flush() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        flush(new EventStageListener<>() {
            @Override
            public void success(Void value) {
                future.complete(null);
            }

            @Override
            public void failure(Throwable cause) {
                future.completeExceptionally(cause);
            }
        });
        return future;
    }

    void flush(EventStageListener<Void> listener);

    default EventStage<Void> flush(EventPromise<Void> promise) {
        if (promise == null) {
            promise = EventPromise.newPromise(executor());
        }
        flush((EventStageListener<Void>) promise);
        return promise;
    }

    /**
     * Reads log entries starting from the specified ID and invokes the provided observer for each
     * entry.
     *
     * @param startId  the ID of the first log entry to read
     * @param limit    the maximum number of log entries to read
     * @param observer the observer to be notified with each log entry, its completion, or any
     *                 errors
     */
    void read(long startId, int limit, LogObserver<T> observer);

    /**
     * Reads log entries starting from the specified offset and ID, and invokes the provided
     * observer for each entry.
     *
     * @param startOffset the offset of the first log entry to read
     * @param startId     the ID of the first log entry to read
     * @param limit       the maximum number of log entries to read
     * @param observer    the observer to be notified with each log entry, its completion, or any
     *                    errors
     */
    void read(long startOffset, long startId, int limit, LogObserver<T> observer);

    /**
     * Reads log entries starting from the specified ID and offset, and invokes the provided
     * observer for each entry.
     *
     * @param idAndOffset the {@link IdAndOffset} object containing the ID and offset of the first
     *                    log entry to read
     * @param limit       the maximum number of log entries to read
     * @param observer    the observer to be notified with each log entry, its completion, or any
     *                    errors
     */
    default void read(IdAndOffset idAndOffset, int limit, LogObserver<T> observer) {
        read(idAndOffset.offset(), idAndOffset.id(), limit, observer);
    }

    /**
     * Cleans up all log segments where the endId is less than the specified ID.
     *
     * @param idBefore the ID before which all log segments will be cleaned up
     */
    void expire(long idBefore);

    /**
     * Returns the {@link EventExecutor} responsible for handling tasks within this
     * {@link EventLogger}.
     *
     * @return the internal {@link EventExecutor}
     */
    EventExecutor executor();
}
