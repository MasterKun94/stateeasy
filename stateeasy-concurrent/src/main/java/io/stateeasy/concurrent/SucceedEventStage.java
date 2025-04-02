package io.stateeasy.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;

/**
 * A sealed class that represents an event stage which has already succeeded with a specific value.
 * This class implements the {@link EventStage} interface and is designed to be used in scenarios
 * where the result of an asynchronous operation is already known and successful.
 *
 * @param <T> The type of the value held by this event stage.
 */
public sealed class SucceedEventStage<T> implements EventStage<T> permits SucceedEventFuture {
    protected final T value;
    protected final EventExecutor executor;

    public SucceedEventStage(T value, EventExecutor executor) {
        this.value = value;
        this.executor = executor;
    }

    protected <P> EventPromise<P> newPromise(EventExecutor executor) {
        return new DefaultEventPromise<>(executor);
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public @Nullable Try<T> getResult() {
        return Try.success(value);
    }

    @Override
    public EventExecutor executor() {
        return executor;
    }

    @Override
    public <P> EventStage<P> map(Function<T, P> func, EventExecutor executor) {
        if (executor.inExecutor()) {
            try {
                return new SucceedEventStage<>(func.apply(value), executor);
            } catch (Throwable e) {
                return new FailedEventStage<>(e, executor);
            }
        } else {
            EventPromise<P> future = newPromise(executor);
            executor.execute(() -> {
                try {
                    future.success(func.apply(value));
                } catch (Throwable e) {
                    future.failure(e);
                }
            });
            return future;
        }
    }

    @Override
    public <P> EventStage<P> flatmap(Function<T, EventStage<P>> func, EventExecutor executor) {
        if (executor.inExecutor()) {
            try {
                return func.apply(value);
            } catch (Throwable e) {
                return new FailedEventFuture<>(e, executor);
            }
        } else {
            EventPromise<P> future = newPromise(executor);
            executor.execute(() -> {
                try {
                    func.apply(value).addListener(future);
                } catch (Throwable e) {
                    future.failure(e);
                }
            });
            return future;
        }
    }

    @Override
    public <P> EventStage<P> transform(Function<Try<T>, Try<P>> transformer,
                                       EventExecutor executor) {
        if (executor.inExecutor()) {
            try {
                return switch (transformer.apply(Try.success(value))) {
                    case Success<P>(P v) -> new SucceedEventStage<>(v, executor);
                    case Failure<?>(Throwable cause) -> new FailedEventStage<>(cause, executor);
                };
            } catch (Throwable e) {
                return new FailedEventStage<>(e, executor);
            }
        } else {
            EventPromise<P> future = newPromise(executor);
            executor.execute(() -> {
                switch (transformer.apply(Try.success(value))) {
                    case Success<P>(P v) -> future.success(v);
                    case Failure<?>(Throwable cause) -> future.failure(cause);
                }
            });
            return future;
        }
    }

    @Override
    public <P> EventStage<P> flatTransform(Function<Try<T>, EventStage<P>> transformer,
                                           EventExecutor executor) {
        if (executor.inExecutor()) {
            try {
                return transformer.apply(Try.success(value));
            } catch (Throwable e) {
                return new FailedEventStage<>(e, executor);
            }
        } else {
            EventPromise<P> future = newPromise(executor);
            executor.execute(() -> {
                try {
                    transformer.apply(Try.success(value)).addListener(future);
                } catch (Throwable e) {
                    future.failure(e);
                }
            });
            return future;
        }
    }

    @Override
    public EventStage<T> addListeners(Collection<EventStageListener<T>> eventStageListeners) {
        if (executor.inExecutor()) {
            for (EventStageListener<T> listener : eventStageListeners) {
                try {
                    listener.success(value);
                } catch (Throwable e) {
                    listener.failure(e);
                }
            }
        } else {
            executor.execute(() -> addListeners(eventStageListeners));
        }
        return this;
    }

    @Override
    public EventFuture<T> toFuture() {
        return new SucceedEventFuture<>(value, executor);
    }
}
