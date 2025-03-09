package io.masterkun.stateeasy.concurrent;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.function.Function;

public class NoopEventPromise<T> implements EventPromise<T> {
    private final EventExecutor executor;

    public NoopEventPromise(EventExecutor executor) {
        this.executor = executor;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public @Nullable Try<T> getResult() {
        throw new UnsupportedOperationException("NoopEventPromise is not support");
    }

    @Override
    public EventExecutor executor() {
        if (executor != null) {
            return executor;
        }
        throw new UnsupportedOperationException("NoopEventPromise is not support");
    }

    @Override
    public <P> EventStage<P> map(Function<T, P> func, EventExecutor executor) {
        throw new UnsupportedOperationException("NoopEventPromise is not support");
    }

    @Override
    public <P> EventStage<P> flatmap(Function<T, EventStage<P>> func, EventExecutor executor) {
        throw new UnsupportedOperationException("NoopEventPromise is not support");
    }

    @Override
    public <P> EventStage<P> transform(Function<Try<T>, Try<P>> transformer, EventExecutor executor) {
        throw new UnsupportedOperationException("NoopEventPromise is not support");
    }

    @Override
    public boolean cancel() {
        return false;
    }

    @Override
    public EventPromise<T> addListeners(Collection<EventStageListener<T>> eventListeners) {
        return this;
    }

    @Override
    public void success(T value) {

    }

    @Override
    public void failure(Throwable cause) {

    }
}
