package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;

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
     * Provides an {@link EventStore} for managing events. The event store is responsible for storing,
     * appending, expiring, and recovering events, as well as flushing the store to ensure all pending
     * events are processed.
     *
     * @param executor the {@link EventExecutor} used for executing event-related tasks
     * @return an instance of {@link EventStore} configured with the provided {@code EventExecutor}
     */
    EventStore<EVENT> eventStore(EventExecutor executor);
}
