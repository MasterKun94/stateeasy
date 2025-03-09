package io.masterkun.stateeasy.concurrent;

import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public interface EventFuture<T> extends EventStage<T>, Future<T> {
    static <T> EventFuture<T> supplyAsync(Callable<T> callable, EventExecutor executor) {
       return (EventFuture<T>) EventStage.supplyAsync(callable, executor);
    }

    static EventFuture<Void> runAsync(Runnable runnable, EventExecutor executor) {
        return (EventFuture<Void>) EventStage.runAsync(runnable, executor);
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

    Try<T> syncResult() throws InterruptedException;

    Try<T> syncResult(long time, TimeUnit unit) throws InterruptedException;

    Try<T> syncUninterruptibly();

    Try<T> syncUninterruptibly(long time, TimeUnit unit);
}
