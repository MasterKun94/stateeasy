package io.stateeasy.indexlogging;

import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.EventPromise;
import io.stateeasy.concurrent.EventStage;
import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.concurrent.EventStageListenerAdaptor;
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
    String name();

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
        write(obj, flush, immediateCallback, new EventStageListenerAdaptor<>(future));
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
            promise = executor().newPromise();
        }
        write(obj, flush, immediateCallback, (EventStageListener<IdAndOffset>) promise);
        return promise;
    }

    /**
     * Asynchronously flushes the log to ensure all pending writes are persisted. This method
     * returns a CompletableFuture that will be completed when the flush operation is complete.
     *
     * @return a CompletableFuture that will be completed with null upon successful completion of
     * the flush, or exceptionally if an error occurs during the flush operation.
     */
    default CompletableFuture<Void> flush() {
        CompletableFuture<Void> future = new CompletableFuture<>();
        flush(new EventStageListenerAdaptor<>(future));
        return future;
    }

    /**
     * Asynchronously flushes the log to ensure all pending writes are persisted. This method will
     * notify the provided listener upon completion or error of the flush operation.
     *
     * @param listener the listener to be notified with the result of the flush operation, or any
     *                 errors
     */
    void flush(EventStageListener<Void> listener);

    /**
     * Asynchronously flushes the log to ensure all pending writes are persisted. If the provided
     * promise is null, a new promise will be created using the executor.
     *
     * @param promise the optional promise to be completed with the result of the flush operation;
     *                if null, a new promise is created
     * @return the promise that will be completed with null upon successful completion of the flush,
     * or exceptionally if an error occurs during the flush operation
     */
    default EventStage<Void> flush(@Nullable EventPromise<Void> promise) {
        if (promise == null) {
            promise = executor().newPromise();
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
     * @param startOffset the offset from which to start reading the event
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
     * Asynchronously reads a single entity by its ID.
     *
     * @param id the ID of the entity to be read
     * @return a CompletableFuture that will be completed with the entity, or null if not found
     */
    default CompletableFuture<@Nullable T> readOne(long id) {
        CompletableFuture<T> future = new CompletableFuture<>();
        readOne(id, new EventStageListenerAdaptor<>(future));
        return future;
    }

    /**
     * Reads a single event with the specified ID and returns a promise that will be completed with
     * the result.
     *
     * @param id      the ID of the event to read
     * @param promise an optional promise to be used for the operation; if null, a new promise is
     *                created
     * @return the promise that will be completed with the result of the read operation, or null if
     * not found
     */
    default EventStage<@Nullable T> readOne(long id, @Nullable EventPromise<T> promise) {
        if (promise == null) {
            promise = executor().newPromise();
        }
        readOne(id, (EventStageListener<T>) promise);
        return promise;
    }

    /**
     * Reads a single entity from the data source based on the specified ID
     *
     * @param id       the unique identifier of the entity to be read
     * @param listener the callback to be notified with the result, which may be null if no entity
     *                 is found
     */
    void readOne(long id, EventStageListener<@Nullable T> listener);

    /**
     * Reads a single event from the specified start offset with the given identifier.
     *
     * @param startOffset the offset from which to start reading the event
     * @param id          the unique identifier of the event
     * @return a CompletableFuture that will be completed with the event, or null if no event is
     * found
     */
    default CompletableFuture<@Nullable T> readOne(long startOffset, long id) {
        CompletableFuture<T> future = new CompletableFuture<>();
        readOne(startOffset, id, new EventStageListenerAdaptor<>(future));
        return future;
    }

    /**
     * Reads a single event from the specified start offset with the given identifier.
     *
     * @param startOffset the offset from which to start reading the event
     * @param id          the unique identifier of the event to read
     * @param promise     the promise to be completed with the result of the read operation, or null
     *                    to create a new one
     * @return the promise that will be completed with the result of the read operation
     */
    default EventStage<@Nullable T> readOne(long startOffset, long id,
                                            @Nullable EventPromise<T> promise) {
        if (promise == null) {
            promise = executor().newPromise();
        }
        readOne(startOffset, id, (EventStageListener<T>) promise);
        return promise;
    }

    /**
     * Reads a single event from the specified start offset with the given identifier.
     *
     * @param startOffset the offset from which to start reading the event
     * @param id          the unique identifier for the event
     * @param listener    the listener to be notified with the read event or null if no event is
     *                    found
     */
    void readOne(long startOffset, long id, EventStageListener<@Nullable T> listener);


    /**
     * Asynchronously expires entries with an ID less than the specified value, invoking the
     * provided listener upon completion.
     * <p>
     * Note: This method does not guarantee that all entries with an ID less than the specified
     * value will be expired. If the data is in the current log segment being written, it will not
     * be expired.
     *
     * @param idBefore the ID threshold; entries with an ID less than this value will be expired,
     * @return a CompletableFuture that will be completed with a Boolean result indicating whether
     * any data was expired (true if data was expired, false otherwise)
     */
    default CompletableFuture<Boolean> expire(long idBefore) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        expire(idBefore, new EventStageListenerAdaptor<>(future));
        return future;
    }


    /**
     * Expires entries with an ID less than the specified value, invoking the provided listener upon
     * completion.
     * <p>
     * Note: This method does not guarantee that all entries with an ID less than the specified
     * value will be expired. If the data is in the current log segment being written, it will not
     * be expired.
     *
     * @param idBefore the ID threshold; entries with an ID less than this value will be expired,
     *                 except those in the current log segment being written
     * @param listener the listener to be notified with a Boolean result indicating whether any data
     *                 was expired (true if data was expired, false otherwise)
     */
    void expire(long idBefore, EventStageListener<Boolean> listener);

    /**
     * Asynchronously expires entries with an ID less than the specified value, invoking the
     * provided promise upon completion.
     * <p>
     * Note: This method does not guarantee that all entries with an ID less than the specified
     * value will be expired. If the data is in the current log segment being written, it will not
     * be expired.
     *
     * @param idBefore the ID threshold; entries with an ID less than this value will be expired,
     *                 except those in the current log segment being written
     * @param promise  the optional promise to be completed with a Boolean result indicating whether
     *                 any data was expired (true if data was expired, false otherwise)
     * @return the promise that will be completed with a Boolean result indicating whether any data
     * was expired (true if data was expired, false otherwise)
     */
    default EventStage<Boolean> expire(long idBefore, EventPromise<Boolean> promise) {
        if (promise == null) {
            promise = executor().newPromise();
        }
        expire(idBefore, (EventStageListener<Boolean>) promise);
        return promise;
    }

    /**
     * Returns the {@link EventExecutor} responsible for handling tasks within this
     * {@link EventLogger}.
     *
     * @return the internal {@link EventExecutor}
     */
    EventExecutor executor();
}
