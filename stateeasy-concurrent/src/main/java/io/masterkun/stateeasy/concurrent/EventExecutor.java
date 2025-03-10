package io.masterkun.stateeasy.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * An interface that extends {@link ScheduledExecutorService} and provides additional methods for managing asynchronous tasks.
 * This sealed interface can only be implemented by {@link SingleThreadEventExecutor}.
 */
public sealed interface EventExecutor extends ScheduledExecutorService
        permits SingleThreadEventExecutor {
    /**
     * Checks if the current thread is the thread that is executing tasks for this {@code EventExecutor}.
     *
     * @return {@code true} if the current thread is the event executor thread, otherwise {@code false}
     */
    boolean inExecutor();

    /**
     * Adds a timeout to the given {@code CompletableFuture}. If the future does not complete within the specified timeout,
     * it will be completed exceptionally with a {@code TimeoutException}.
     *
     * @param <T>     the type of the result of the future
     * @param future  the {@code CompletableFuture} to which the timeout is applied
     * @param timeout the duration of the timeout
     * @param unit    the time unit of the timeout
     * @return a new {@code CompletableFuture} that completes with the result of the original future or exceptionally with a {@code TimeoutException} if the timeout elapses
     */
    default <T> CompletableFuture<T> timeout(CompletableFuture<T> future, long timeout, TimeUnit unit) {
        if (future.isDone()) {
            return future;
        }
        ScheduledFuture<?> schedule = schedule(() -> future.completeExceptionally(new TimeoutException()), timeout, unit);
        return future.whenComplete((t, e) -> {
            if (!schedule.isCancelled()) {
                schedule.cancel(false);
            }
        });
    }

    /**
     * Adds a timeout to the given {@code EventPromise}. If the promise does not complete within the specified timeout,
     * it will be completed exceptionally with a {@code TimeoutException}.
     *
     * @param <T>     the type of the result of the promise
     * @param promise the {@code EventPromise} to which the timeout is applied
     * @param timeout the duration of the timeout
     * @param unit    the time unit of the timeout
     * @return a new {@code EventPromise} that completes with the result of the original promise or exceptionally with a {@code TimeoutException} if the timeout elapses
     */
    default <T> EventPromise<T> timeout(EventPromise<T> promise, long timeout, TimeUnit unit) {
        if (promise.isDone()) {
            return promise;
        }
        ScheduledFuture<?> schedule = schedule(() -> promise.failure(new TimeoutException()), timeout, unit);
        return promise.addListener(new EventStageListenerAdaptor<>() {
            @Override
            protected void complete(T value, Throwable cause) {
                if (!schedule.isCancelled()) {
                    schedule.cancel(false);
                }
            }
        });
    }
}
