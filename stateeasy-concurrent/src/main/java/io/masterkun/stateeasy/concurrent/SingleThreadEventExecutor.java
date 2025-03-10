package io.masterkun.stateeasy.concurrent;

/**
 * A non-sealed interface that extends {@link EventExecutor} and represents an event executor that processes all its tasks in a single thread.
 * This ensures that tasks submitted to the executor are executed sequentially, and no concurrent execution of tasks can occur.
 */
public non-sealed interface SingleThreadEventExecutor extends EventExecutor {
}
