package io.masterkun.stateeasy.concurrent;

import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Try<T> permits Success, Failure {
    static <T> Success<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Failure<T> failure(Throwable cause) {
        return new Failure<>(cause);
    }

    static <T> Try<T> apply(Supplier<T> supplier) {
        try {
            return success(supplier.get());
        } catch (Throwable e) {
            return failure(e);
        }
    }

    static <T, P> Try<P> apply(Function<T, P> func, T param) {
        try {
            return success(func.apply(param));
        } catch (Throwable e) {
            return failure(e);
        }
    }

    boolean isSuccess();

    boolean isFailure();

    T value();

    Throwable cause();

    <P> Try<P> map(Function<T, P> func);

    <P> Try<P> flatmap(Function<T, Try<P>> func);

    <P> Try<P> transform(Function<Try<T>, P> func);

    void notify(EventPromise<T> promise);
}
