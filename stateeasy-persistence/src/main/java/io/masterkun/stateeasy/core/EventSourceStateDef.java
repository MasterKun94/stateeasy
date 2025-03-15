package io.masterkun.stateeasy.core;

import java.util.concurrent.ScheduledExecutorService;

/**
 * Defines the contract for a state definition that is specifically designed to work with event
 * sources. This interface extends {@link StateDef} and adds the capability to manage an event
 * store, which is responsible for storing and managing events that can update the state.
 *
 * @param <STATE> the type of the state being managed
 * @param <EVENT> the type of the events that can update the state
 */
public interface EventSourceStateDef<STATE, EVENT> extends StateDef<STATE, EVENT> {
    /**
     * Returns an {@code EventStore} instance for managing and storing events. The provided
     * {@code ScheduledExecutorService} is used to handle the execution of asynchronous tasks
     * related to event management, such as appending, expiring, and recovering events.
     *
     * @param executor the {@code ScheduledExecutorService} that will be used to execute
     *                 asynchronous tasks
     * @return an {@code EventStore<EVENT>} instance for managing and storing events
     */
    EventStore<EVENT> eventStore(ScheduledExecutorService executor);
}
