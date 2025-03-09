package io.masterkun.stateeasy.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface EventStage<T> {
    static <T> EventStage<T> supplyAsync(Callable<T> callable, EventExecutor executor) {
        EventPromise<T> promise = newPromise(executor);
        executor.execute(() -> {
            if (promise.isCancelled()) {
                return;
            }
            try {
                promise.success(callable.call());
            } catch (Throwable cause) {
                promise.failure(cause);
            }
        });
        return promise;
    }

    static EventStage<Void> runAsync(Runnable runnable, EventExecutor executor) {
        EventPromise<Void> promise = newPromise(executor);
        executor.execute(() -> {
            if (promise.isCancelled()) {
                return;
            }
            try {
                runnable.run();
                promise.success(null);
            } catch (Throwable cause) {
                promise.failure(cause);
            }
        });
        return promise;
    }

    static <T> EventPromise<T> newPromise(EventExecutor executor) {
        return new DefaultEventFuture<>(executor);
    }

    static <T> EventPromise<T> noopPromise(EventExecutor executor) {
        return new NoopEventPromise<>(executor);
    }

    boolean isDone();

    boolean isCancelled();

    boolean isSuccess();

    boolean isFailure();

    @Nullable
    Try<T> getResult();

    EventExecutor executor();

    default <P> EventStage<P> map(Function<T, P> func) {
        return map(func, executor());
    }

    <P> EventStage<P> map(Function<T, P> func, EventExecutor executor);

    default <P> EventStage<P> flatmap(Function<T, EventStage<P>> func) {
        return flatmap(func, executor());
    }

    <P> EventStage<P> flatmap(Function<T, EventStage<P>> func, EventExecutor executor);

    default <P> EventStage<P> transform(Function<Try<T>, Try<P>> transformer) {
        return transform(transformer, executor());
    }

    <P> EventStage<P> transform(Function<Try<T>, Try<P>> transformer, EventExecutor executor);

    default EventStage<T> addListener(EventStageListener<T> listener) {
        return addListeners(Collections.singletonList(listener));
    }

    EventStage<T> addListeners(Collection<EventStageListener<T>> listeners);

    default CompletableFuture<T> toCompletableFuture() {
        CompletableFuture<T> future = new CompletableFuture<>();
        addListener(new EventStageListener<>() {
            @Override
            public void success(T value) {
                future.complete(value);
            }

            @Override
            public void failure(Throwable cause) {
                if (cause instanceof CancellationException) {
                    future.cancel(false);
                } else {
                    future.completeExceptionally(cause);
                }
            }
        });
        return future;
    }

    default EventFuture<T> toFuture() {
        return new DefaultEventFuture<>(this);
    }
}
