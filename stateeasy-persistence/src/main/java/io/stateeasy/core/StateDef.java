package io.stateeasy.core;

import io.stateeasy.concurrent.EventExecutor;

/**
 * Defines the contract for a state definition, which is used to manage the lifecycle of a state
 * within an application. The state can be updated based on incoming events, and its snapshots can
 * be configured and stored.
 *
 * @param <STATE> the type of the state being managed
 * @param <EVENT> the type of the events that can update the state
 */
public interface StateDef<STATE, EVENT> {
    /**
     * Returns the name of the state. Each StateDef instance's name must be unique and cannot be
     * repeated.
     *
     * @return the name of the state as a String
     */
    String name();

    /**
     * Returns the configuration for state snapshots, including settings such as the interval
     * between snapshots, the maximum number of messages before a snapshot is taken, and whether
     * snapshots should automatically expire.
     *
     * @return the {@code SnapshotConfig} object containing the current snapshot configuration
     */
    SnapshotConfig snapshotConfig();

    /**
     * Provides the initial state for the state management system. This method is called to obtain
     * the starting state before any events or updates are applied.
     *
     * @return the initial state of type {@code STATE}
     */
    STATE initialState();

    /**
     * Updates the current state based on the incoming event message.
     *
     * @param state the current state of type {@code STATE}
     * @param msg   the incoming event message of type {@code EVENT} that triggers the state update
     * @return the updated state of type {@code STATE} after processing the event message
     */
    STATE update(STATE state, EVENT msg);

    /**
     * Creates and returns a {@code StateStore} instance for managing the state of this
     * {@code StateDef}. The provided {@code EventExecutor} is used to handle the execution of
     * asynchronous tasks related to state management, such as writing and reading state snapshots.
     *
     * @param executor the {@code EventExecutor} that will be used to execute asynchronous tasks
     * @return a {@code StateStore<STATE>} instance for managing the state
     */
    StateStore<STATE> stateStore(EventExecutor executor);
}
