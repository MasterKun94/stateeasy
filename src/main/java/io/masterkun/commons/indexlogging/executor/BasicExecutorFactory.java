package io.masterkun.commons.indexlogging.executor;

import org.jetbrains.annotations.Nullable;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;

public class BasicExecutorFactory implements ExecutorFactory {

    @Override
    public ScheduledExecutorService createSingleThreadExecutor(@Nullable ThreadFactory threadFactory) {
        if (threadFactory == null) {
            return Executors.newSingleThreadScheduledExecutor();
        }
        return Executors.newSingleThreadScheduledExecutor(threadFactory);
    }

    @Override
    public int priority() {
        return 0;
    }

    @Override
    public boolean available() {
        return true;
    }
}
