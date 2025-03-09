package io.masterkun.stateeasy.concurrent;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CancellationException;

public interface EventPromise<T> extends EventStage<T>, EventStageListener<T> {
    static <T> EventPromise<T> newPromise(EventExecutor executor) {
        return new DefaultEventPromise<>(executor);
    }
    static <T> EventPromise<T> noop(EventExecutor executor) {
        return new NoopEventPromise<>(executor);
    }

    boolean cancel();

    @Override
    default EventPromise<T> addListener(EventStageListener<T> listener) {
        return addListeners(Collections.singletonList(listener));
    }

    @Override
    EventPromise<T> addListeners(Collection<EventStageListener<T>> eventListeners);
}
