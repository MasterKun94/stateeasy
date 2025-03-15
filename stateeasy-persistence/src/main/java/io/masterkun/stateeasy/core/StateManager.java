package io.masterkun.stateeasy.core;

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
 * @param <MSG>   the type of the messages being processed
 */
public sealed interface StateManager<STATE, MSG> permits SnapshotStateManager {
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
     * Sends a message to the state manager for processing.
     * <p>
     * This method allows sending a message of type {@code MSG} to the state manager. The state
     * manager will process the message and update its internal state accordingly. The method
     * returns an {@code EventStage<Void>} that can be used to track the completion of the message
     * processing.
     *
     * @param msg the message to be sent to the state manager
     * @return an {@code EventStage<Void>} representing the asynchronous message processing
     * operation
     */
    EventStage<Void> send(MSG msg);

    /**
     * Sends a message to the state manager and queries the state after the message is processed.
     * <p>
     * This method sends a message of type {@code MSG} to the state manager, processes it, and then
     * applies the provided function to the updated state. The result of the function is returned as
     * an {@code EventStage<T>}, which can be used to track the completion of the operation and
     * retrieve the result.
     *
     * @param <T>      the type of the result returned by the function
     * @param msg      the message to be sent to the state manager
     * @param function the function to apply to the state after the message is processed
     * @return an {@code EventStage<T>} representing the asynchronous operation and the result of
     * the function
     */
    <T> EventStage<T> sendAndQuery(MSG msg, Function<STATE, T> function);

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
