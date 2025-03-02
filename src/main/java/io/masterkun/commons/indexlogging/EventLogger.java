package io.masterkun.commons.indexlogging;

import java.util.concurrent.CompletableFuture;

/**
 * An interface for logging events with support for asynchronous writing and reading of log entries.
 * The implementation of this interface is responsible for managing the persistence, retrieval,
 * and lifecycle of log entries.
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
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID and offset
     * of the written log entry once the write operation is complete
     */
    default CompletableFuture<IdAndOffset> write(T obj) {
        return write(obj, false);
    }

    /**
     * Asynchronously writes the given object to the log with an option to flush.
     *
     * @param obj   the object to be written to the log
     * @param flush if true, the write operation will be followed by a flush to ensure data is persisted
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID and offset
     * of the written log entry once the write operation is complete
     */
    default CompletableFuture<IdAndOffset> write(T obj, boolean flush) {
        return write(obj, flush, false);
    }

    /**
     * Asynchronously writes the given object to the log with options for flushing and immediate callback.
     *
     * @param obj               the object to be written to the log
     * @param flush             if true, the write operation will be followed by a flush to ensure data is persisted
     * @param immediateCallback if true, the callback will be invoked immediately after the write operation,
     *                          otherwise, it will be invoked after the data is persisted
     * @return a CompletableFuture that will be completed with an IdAndOffset representing the ID and offset
     * of the written log entry once the write operation is complete
     */
    CompletableFuture<IdAndOffset> write(T obj, boolean flush, boolean immediateCallback);

    /**
     * Reads log entries starting from the specified ID and invokes the provided observer for each entry.
     *
     * @param startId  the ID of the first log entry to read
     * @param limit    the maximum number of log entries to read
     * @param observer the observer to be notified with each log entry, its completion, or any errors
     */
    void read(long startId, int limit, LogObserver<T> observer);

    /**
     * Reads log entries starting from the specified offset and ID, and invokes the provided observer for each entry.
     *
     * @param startOffset the offset of the first log entry to read
     * @param startId     the ID of the first log entry to read
     * @param limit       the maximum number of log entries to read
     * @param observer    the observer to be notified with each log entry, its completion, or any errors
     */
    void read(long startOffset, long startId, int limit, LogObserver<T> observer);

    /**
     * Reads log entries starting from the specified ID and offset, and invokes the provided observer for each entry.
     *
     * @param idAndOffset the {@link IdAndOffset} object containing the ID and offset of the first log entry to read
     * @param limit       the maximum number of log entries to read
     * @param observer    the observer to be notified with each log entry, its completion, or any errors
     */
    default void read(IdAndOffset idAndOffset, int limit, LogObserver<T> observer) {
        read(idAndOffset.offset(), idAndOffset.id(), limit, observer);
    }
}
