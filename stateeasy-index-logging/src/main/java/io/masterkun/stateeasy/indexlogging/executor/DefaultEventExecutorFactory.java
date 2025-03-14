package io.masterkun.stateeasy.indexlogging.executor;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutorThreadFactory;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ThreadFactory;

public class DefaultEventExecutorFactory implements EventExecutorFactory {
    @Override
    public EventExecutor createEventExecutor(@Nullable EventExecutorThreadFactory threadFactory) {
        return new DefaultSingleThreadEventExecutor(threadFactory);
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean available() {
        return true;
    }
}
