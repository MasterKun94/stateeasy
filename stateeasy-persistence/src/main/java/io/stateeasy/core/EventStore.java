package io.stateeasy.core;

import io.stateeasy.concurrent.EventStageListener;

/**
 * Interface representing a store for events, providing methods to initialize, flush, append,
 * expire, and recover events. It is designed to work with event-driven systems where events are
 * stored and managed over time.
 *
 * @param <EVENT> the type of events that this store will manage
 */
public interface EventStore<EVENT> {

    /**
     * Initializes the event store with the provided state definition and an event stage listener.
     * This method sets up the initial configuration and prepares the event store for managing
     * events.
     *
     * @param stateDef the state definition that provides the initial state, update logic, and
     *                 snapshot configuration
     * @param listener the event stage listener to be notified upon the completion of the
     *                 initialization process
     */
    void initialize(EventSourceStateDef<?, EVENT> stateDef, EventStageListener<Void> listener);

    /**
     * Flushes the event store, ensuring that all pending events are processed and any necessary
     * state changes are committed. This method is asynchronous and will notify the provided
     * listener upon completion.
     *
     * @param listener the event stage listener to be notified upon the completion of the flush
     *                 operation, either with a success or failure
     */
    void flush(EventStageListener<Void> listener);

    /**
     * Appends an event to the store and notifies the provided listener upon completion.
     *
     * @param event    the event to be appended to the store
     * @param listener the listener to be notified of the success or failure of the append
     *                 operation. The success method will be called with an
     *                 {@code EventHolder<EVENT>} containing the event ID and the event, while the
     *                 failure method will be called with a {@code Throwable} if the operation
     *                 fails
     */
    void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener);

    /**
     * Expires events in the store that have an event ID less than the specified
     * {@code expireBeforeEventId}. This method is asynchronous and will notify the provided
     * listener upon completion.
     *
     * @param expireBeforeEventId the event ID before which all events should be expired
     * @param listener            the event stage listener to be notified upon the completion of the
     *                            expiration process, with a boolean value indicating success or
     *                            failure
     */
    void expire(long expireBeforeEventId, EventStageListener<Boolean> listener);

    /**
     * Recovers the state of the event store from a specific event ID and notifies the provided
     * observer with the recovered events. This method is useful for resuming processing from a
     * certain point in the event stream.
     *
     * @param recoverAtEventId the event ID from which to start recovery
     * @param observer         the observer that will be notified with the recovered events,
     *                         completion, or any errors that occur during the recovery process
     */
    void recover(long recoverAtEventId, EventObserver<EVENT> observer);

    interface EventObserver<EVENT> {
        void onEvent(EventHolder<EVENT> event);

        void onComplete();

        void onError(Throwable error);
    }

    record EventHolder<EVENT>(long eventId, EVENT event) {
    }
}
