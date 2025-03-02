package io.masterkun.commons.indexlogging.executor;

import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public interface ExecutorFactory {

    List<ExecutorFactory> ALL_INSTANCE = ServiceLoader.load(ExecutorFactory.class)
            .stream()
            .map(ServiceLoader.Provider::get)
            .filter(ExecutorFactory::available)
            .sorted(Comparator.comparingInt(ExecutorFactory::priority).reversed())
            .toList();
    ExecutorFactory DEFAULT = ALL_INSTANCE.get(0);

    ScheduledExecutorService createSingleThreadExecutor(@Nullable ThreadFactory threadFactory);

    int priority();

    boolean available();
}
