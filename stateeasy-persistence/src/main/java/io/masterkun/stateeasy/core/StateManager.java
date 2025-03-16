package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStage;

import java.util.function.Function;

/**
 * Defines a contract for managing state and handling messages in an asynchronous manner.
 * <p>
 * Implementations of this interface are responsible for starting, processing messages, querying the
 * state, and shutting down the state management process. The state and message types are generic,
 * allowing for flexibility in the type of state and messages that can be managed.
 *
 * @param <STATE> the type of the state being managed
 * @param <EVENT> the type of the events handled by this state manager
 */
public sealed interface StateManager<STATE, EVENT> permits SnapshotStateManager {
    /**
     * Creates a new {@code StateManager} instance based on the provided state definition and event
     * executor.
     *
     * @param <STATE>  the type of the state being managed
     * @param <EVENT>  the type of the events that can update the state
     * @param stateDef the state definition that defines the behavior and initial state of the state
     *                 manager
     * @param executor the event executor used to handle the execution of asynchronous tasks related
     *                 to state management
     * @return a new {@code StateManager<STATE, EVENT>} instance
     */
    static <STATE, EVENT> StateManager<STATE, EVENT> create(StateDef<STATE, EVENT> stateDef,
                                                            EventExecutor executor) {
        return StateManagerPool.INSTANCE.create(stateDef, executor);
    }

    /**
     * Returns the name of the state. Each StateDef instance's name must be unique and cannot be
     * repeated.
     *
     * @return the name of the state as a String
     */
    String name();

    /**
     * Initiates the state management process.
     * <p>
     * This method starts the state management, making it ready to handle messages and manage state.
     * It returns an {@code EventStage} that can be used to track the completion of the start
     * operation.
     *
     * @return an {@code EventStage<Void>} representing the asynchronous start operation
     */
    EventStage<Void> start();

    /**
     * Sends an event to the state manager for processing.
     * <p>
     * This method allows you to send an event of type {@code EVENT} to the state manager. The event
     * will be processed according to the current state and any defined state transitions. The
     * method returns an {@code EventStage<Void>} which can be used to track the completion of the
     * event processing.
     *
     * @param event the event to be sent to the state manager
     * @return an {@code EventStage<Void>} representing the asynchronous operation
     */
    EventStage<Void> send(EVENT event);

    /**
     * Sends an event to the state manager and queries the resulting state.
     * <p>
     * This method sends an event of type {@code EVENT} to the state manager for processing. After
     * the event is processed, the provided function is applied to the resulting state. The method
     * returns an {@code EventStage<T>} which can be used to track the completion of the event
     * processing and to retrieve the result of the function.
     *
     * @param <T>      the type of the result returned by the function
     * @param event    the event to be sent to the state manager
     * @param function the function to apply to the resulting state
     * @return an {@code EventStage<T>} representing the asynchronous operation and the result of
     * the function
     */
    <T> EventStage<T> sendAndQuery(EVENT event, Function<STATE, T> function);

    /**
     * Queries the current state and applies the provided function to it, returning the result.
     * <p>
     * This method provides a fast way to query the current state and apply a function to it. The
     * function is applied to the current state, and the result is returned as an
     * {@code EventStage<T>}, which can be used to track the completion of the operation and
     * retrieve the result.
     *
     * @param <T>      the type of the result returned by the function
     * @param function the function to apply to the current state
     * @return an {@code EventStage<T>} representing the asynchronous operation and the result of
     * the function
     */
    <T> EventStage<T> queryFast(Function<STATE, T> function);

    /**
     * Queries the current state and applies the provided function to it, returning the result.
     * <p>
     * This method provides a way to query the current state and apply a function to it. The
     * function is applied to the current state, and the result is returned as an
     * {@code EventStage<T>}, which can be used to track the completion of the operation and
     * retrieve the result.
     *
     * @param <T>      the type of the result returned by the function
     * @param function the function to apply to the current state
     * @return an {@code EventStage<T>} representing the asynchronous operation and the result of
     * the function
     */
    <T> EventStage<T> query(Function<STATE, T> function);

    /**
     * Initiates the shutdown process for the state manager.
     * <p>
     * This method begins the shutdown process, stopping the state management and processing of any
     * further messages. It returns an {@code EventStage} that can be used to track the completion
     * of the shutdown operation.
     *
     * @return an {@code EventStage<Void>} representing the asynchronous shutdown operation
     */
    EventStage<Void> shutdown();
}
