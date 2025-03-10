package io.masterkun.stateeasy.concurrent;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public sealed interface EventExecutor extends ScheduledExecutorService
        permits SingleThreadEventExecutor {
    boolean inExecutor();

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
