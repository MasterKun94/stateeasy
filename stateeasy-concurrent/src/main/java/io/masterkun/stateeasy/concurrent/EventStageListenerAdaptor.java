package io.masterkun.stateeasy.concurrent;

/**
 * An abstract implementation of the {@code EventStageListener<T>} interface, designed to simplify
 * the process of handling both success and failure outcomes of an event stage. This adaptor
 * provides a single method, {@code complete(T, Throwable)}, which is called in both success and
 * failure scenarios, allowing for more concise and centralized error and success handling.
 *
 * @param <T> the type of the value produced in case of success
 */
public abstract class EventStageListenerAdaptor<T> implements EventStageListener<T> {
    @Override
    public final void success(T value) {
        complete(value, null);
    }

    @Override
    public final void failure(Throwable cause) {
        complete(null, cause);
    }

    /**
     * Completes the event stage by handling both success and failure outcomes. This method is
     * called with a value in case of success, and with a cause in case of failure.
     *
     * @param value the value produced in case of success, or null if the event stage failed
     * @param cause the throwable representing the cause of the failure, or null if the event stage
     *              succeeded
     */
    protected abstract void complete(T value, Throwable cause);
}
