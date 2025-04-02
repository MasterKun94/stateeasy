package io.stateeasy.indexlogging;

/**
 * An interface for observing and reacting to events in a logging system. Implementations of this
 * interface are used to handle log entries as they are processed, providing callbacks for when new
 * log entries are received, when the log processing is complete, and when an error occurs.
 *
 * <p>The {@code LogObserver} interface is generic, allowing it to be used with different types of
 * log values. This makes it flexible for various logging scenarios, such as handling string
 * messages, binary data, or other custom log entry types.
 *
 * @param <T> the type of the value stored in the log entry
 */
public interface LogObserver<T> {
    /**
     * Notifies the observer that a new log entry has been received.
     *
     * @param id     the unique identifier of the log entry
     * @param offset the offset of the log entry within the log
     * @param value  the value of the log entry, which can be of any type
     */
    void onNext(long id, long offset, T value);

    /**
     * Notifies the observer that the log processing is complete.
     *
     * @param nextId     the ID of the next expected log entry
     * @param nextOffset the offset of the next expected log entry
     */
    void onComplete(long nextId, long nextOffset);

    /**
     * Notifies the observer that an error has occurred during the log processing.
     *
     * @param e the {@link Throwable} representing the error that occurred
     */
    void onError(Throwable e);
}
