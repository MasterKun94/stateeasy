package io.masterkun.commons.indexlogging.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class SingleThreadEventExecutorFactory implements ExecutorFactory {
    @Override
    public ScheduledExecutorService createSingleThreadExecutor(@Nullable ThreadFactory threadFactory) {
        return new SingleThreadEventExecutor(threadFactory);
    }

    @Override
    public int priority() {
        return 11;
    }

    @Override
    public boolean available() {
        return true;
    }
}
