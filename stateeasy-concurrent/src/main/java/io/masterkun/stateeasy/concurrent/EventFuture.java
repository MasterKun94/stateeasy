package io.masterkun.stateeasy.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Represents a future that is also an event stage, allowing for the execution of asynchronous tasks
 * and transformation of results. This interface extends both {@code EventStage} and {@code Future},
 * providing a way to handle asynchronous computations with event-driven capabilities.
 *
 * @param <T> the type of the result produced by the asynchronous computation
 */
public non-sealed interface EventFuture<T> extends EventStage<T>, Future<T> {
    /**
     * Schedules the provided callable to be executed asynchronously using the specified event
     * executor.
     *
     * @param <T>      the type of the result produced by the callable
     * @param callable the callable to be executed asynchronously
     * @param executor the event executor on which the callable will be executed
     * @return an {@code EventFuture} that will be completed with the result of the callable, or
     * exceptionally if an error occurs
     */
    static <T> EventFuture<T> supplyAsync(Callable<T> callable, EventExecutor executor) {
        return EventUtils.supplyAsync(callable, new DefaultEventFuture<>(executor));
    }

    /**
     * Schedules the provided runnable to be executed asynchronously using the specified event
     * executor.
     *
     * @param runnable the runnable to be executed asynchronously
     * @param executor the event executor on which the runnable will be executed
     * @return an {@code EventFuture} that will be completed when the runnable completes, or
     * exceptionally if an error occurs
     */
    static EventFuture<Void> runAsync(Runnable runnable, EventExecutor executor) {
        return EventUtils.runAsync(runnable, new DefaultEventFuture<>(executor));
    }

    /**
     * Creates a new {@code EventPromise} with the specified {@code EventExecutor}.
     *
     * @param executor the {@code EventExecutor} to be used for executing the event promise
     * @return a new instance of {@code EventPromise} with the given executor
     */
    static <T> EventPromise<T> newPromise(EventExecutor executor) {
        return new DefaultEventFuture<>(executor);
    }

    EventExecutor executor();

    default <P> EventFuture<P> map(Function<T, P> func) {
        return map(func, executor());
    }

    <P> EventFuture<P> map(Function<T, P> func, EventExecutor executor);

    default <P> EventFuture<P> flatmap(Function<T, EventStage<P>> func) {
        return flatmap(func, executor());
    }

    <P> EventFuture<P> flatmap(Function<T, EventStage<P>> func, EventExecutor executor);

    @Override
    default <P> EventFuture<P> transform(Function<Try<T>, Try<P>> transformer) {
        return transform(transformer, executor());
    }

    @Override
    <P> EventFuture<P> transform(Function<Try<T>, Try<P>> transformer, EventExecutor executor);

    @Override
    EventFuture<T> addListeners(Collection<EventStageListener<T>> eventStageListeners);

    @Override
    default EventFuture<T> toFuture() {
        return this;
    }

    /**
     * Waits for the computation to complete and returns a {@code Try} instance that represents the
     * result of the computation. If the computation completes successfully, a {@code Success}
     * instance is returned. If the computation fails, a {@code Failure} instance is returned. This
     * method will block until the computation is complete or the thread is interrupted.
     *
     * @return a {@code Try} instance containing the result of the computation
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    Try<T> syncResult() throws InterruptedException;

    /**
     * Waits for the computation to complete and returns a {@code Try} instance that represents the
     * result of the computation. If the computation completes successfully, a {@code Success}
     * instance is returned. If the computation fails, a {@code Failure} instance is returned. This
     * method will block until the computation is complete, the specified timeout has elapsed, or
     * the thread is interrupted.
     *
     * @param time the maximum time to wait for the computation to complete
     * @param unit the time unit of the timeout argument
     * @return a {@code Try} instance containing the result of the computation
     * @throws InterruptedException if the current thread is interrupted while waiting
     */
    Try<T> syncResult(long time, TimeUnit unit) throws InterruptedException;

    /**
     * Waits for the computation to complete and returns a {@code Try} instance that represents the
     * result of the computation. If the computation completes successfully, a {@code Success}
     * instance is returned. If the computation fails, a {@code Failure} instance is returned. This
     * method will block until the computation is complete, ignoring any interruptions to the
     * current thread.
     *
     * @return a {@code Try} instance containing the result of the computation
     */
    Try<T> syncUninterruptibly();

    /**
     * Waits for the computation to complete and returns a {@code Try} instance that represents the
     * result of the computation. If the computation completes successfully, a {@code Success}
     * instance is returned. If the computation fails, a {@code Failure} instance is returned. This
     * method will block until the computation is complete, the specified timeout has elapsed, or
     * the thread is interrupted. Unlike other sync methods, this method ignores interruptions to
     * the current thread.
     *
     * @param time the maximum time to wait for the computation to complete
     * @param unit the time unit of the timeout argument
     * @return a {@code Try} instance containing the result of the computation
     */
    Try<T> syncUninterruptibly(long time, TimeUnit unit);
}
