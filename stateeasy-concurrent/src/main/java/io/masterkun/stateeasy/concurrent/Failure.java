package io.masterkun.stateeasy.concurrent;

import java.util.function.Function;

public record Failure<T>(Throwable cause) implements Try<T> {
    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public boolean isFailure() {
        return true;
    }

    @Override
    public T value() {
        throw new RuntimeException(cause);
    }

    @SuppressWarnings("unchecked")
    public <P> Try<P> cast() {
        return (Try<P>) this;
    }

    @Override
    public <P> Try<P> map(Function<T, P> func) {
        return cast();
    }

    @Override
    public <P> Try<P> flatmap(Function<T, Try<P>> func) {
        return cast();
    }

    @Override
    public <P> Try<P> transform(Function<Try<T>, P> func) {
        return Try.apply(func, this);
    }

    @Override
    public void notify(EventPromise<T> promise) {
        promise.failure(cause);
    }
}
