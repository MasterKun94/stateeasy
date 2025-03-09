package io.masterkun.stateeasy.indexlogging.executor;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ThreadFactory;

public interface EventExecutorFactory {

    List<EventExecutorFactory> ALL_INSTANCE = ServiceLoader.load(EventExecutorFactory.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(EventExecutorFactory::available)
            .sorted(Comparator.comparingInt(EventExecutorFactory::priority).reversed())
            .toList();
    EventExecutorFactory DEFAULT = ALL_INSTANCE.get(0);

    EventExecutor createEventExecutor(@Nullable ThreadFactory threadFactory);

    int priority();

    boolean available();
}
