package io.stateeasy.indexlogging.executor;

import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.EventExecutorThreadFactory;
import org.jetbrains.annotations.Nullable;

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
