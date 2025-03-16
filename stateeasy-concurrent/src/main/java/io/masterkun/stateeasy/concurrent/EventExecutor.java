package io.masterkun.stateeasy.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A sealed interface that extends {@link ScheduledExecutorService} and is designed to handle the
 * execution of events. This interface provides methods for managing event execution, including
 * adding timeouts to futures and promises, and creating new promises.
 *
 * <p>Implementations of this interface must be permitted, with {@link SingleThreadEventExecutor}
 * being one such example.
 */
public sealed interface EventExecutor extends ScheduledExecutorService
        permits SingleThreadEventExecutor {

    /**
     * Returns the {@code EventExecutor} that is currently executing the calling thread, if the
     * current thread is an instance of {@code EventExecutorThread}. If the current thread is not an
     * {@code EventExecutorThread}, this method returns {@code null}.
     *
     * @return the current {@code EventExecutor} if the current thread is an
     * {@code EventExecutorThread}, otherwise {@code null}
     */
    @Nullable
    static EventExecutor currentExecutor() {
        if (Thread.currentThread() instanceof EventExecutorThread et) {
            return et.getOwnerExecutor();
        } else {
            return null;
        }
    }

    /**
     * Checks if the current thread is the thread that is executing tasks for this
     * {@code EventExecutor}.
     *
     * @return {@code true} if the current thread is the event executor thread, otherwise
     * {@code false}
     */
    boolean inExecutor();

    /**
     * Adds a timeout to the given {@code CompletableFuture}. If the future does not complete within
     * the specified timeout, it will be completed exceptionally with a {@code TimeoutException}.
     *
     * @param <T>     the type of the result of the future
     * @param future  the {@code CompletableFuture} to which the timeout is applied
     * @param timeout the duration of the timeout
     * @param unit    the time unit of the timeout
     * @return a new {@code CompletableFuture} that completes with the result of the original future
     * or exceptionally with a {@code TimeoutException} if the timeout elapses
     */
    default <T> CompletableFuture<T> timeout(CompletableFuture<T> future, long timeout,
                                             TimeUnit unit) {
        if (future.isDone()) {
            return future;
        }
        ScheduledFuture<?> schedule =
                schedule(() -> future.completeExceptionally(new TimeoutException()), timeout,
                        unit);
        return future.whenComplete((t, e) -> {
            if (!schedule.isCancelled()) {
                schedule.cancel(false);
            }
        });
    }

    /**
     * Adds a timeout to the given {@code EventPromise}. If the promise does not complete within the
     * specified timeout, it will be completed exceptionally with a {@code TimeoutException}.
     *
     * @param <T>     the type of the result of the promise
     * @param promise the {@code EventPromise} to which the timeout is applied
     * @param timeout the duration of the timeout
     * @param unit    the time unit of the timeout
     * @return a new {@code EventPromise} that completes with the result of the original promise or
     * exceptionally with a {@code TimeoutException} if the timeout elapses
     */
    default <T> EventPromise<T> timeout(EventPromise<T> promise, long timeout, TimeUnit unit) {
        if (promise.isDone()) {
            return promise;
        }
        ScheduledFuture<?> schedule = schedule(() -> promise.failure(new TimeoutException()),
                timeout, unit);
        return promise.addListener(new EventStageListener<T>() {
            @Override
            public void success(T value) {
                if (!schedule.isCancelled()) {
                    schedule.cancel(false);
                }
            }

            @Override
            public void failure(Throwable cause) {
                if (!schedule.isCancelled()) {
                    schedule.cancel(false);
                }
            }
        });
    }

    /**
     * Creates a new {@code EventPromise} with the current {@code EventExecutor}.
     *
     * @param <T> the type of the result that the event promise will provide
     * @return a new instance of {@code EventPromise} with the current executor
     */
    default <T> EventPromise<T> newPromise() {
        return EventPromise.newPromise(this);
    }

    /**
     * Creates a new {@code EventPromise} that is backed by a future, using the current
     * {@code EventExecutor}.
     *
     * @param <T> the type of the result that the event promise will provide
     * @return a new instance of {@code EventPromise} that is associated with a future and the
     * current executor
     */
    default <T> EventPromise<T> newFuturePromise() {
        return EventFuture.newPromise(this);
    }

    CompletableFuture<Void> shutdownAsync();
}
