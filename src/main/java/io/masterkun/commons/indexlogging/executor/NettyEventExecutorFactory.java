package io.masterkun.commons.indexlogging.executor;

import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

public class NettyEventExecutorFactory implements ExecutorFactory {
    private static final boolean available;
    private static final Function<ThreadFactory, ScheduledExecutorService> func;
    private static final List<String> EXECUTOR_TYPES = List.of(
            "io.netty.util.concurrent.DefaultEventExecutor",
            "io.grpc.netty.shaded.io.netty.util.concurrent.DefaultEventExecutor"
    );

    static {
        boolean b = false;
        Function<ThreadFactory, ScheduledExecutorService> f = null;
        for (String type : EXECUTOR_TYPES) {
            try {
                Class<?> cls = Class.forName(type);
                b = true;
                Constructor<?> constructor = cls.getConstructor(ThreadFactory.class);
                f = th -> {
                    try {
                        return (ScheduledExecutorService) constructor.newInstance(th);
                    } catch (InstantiationException | InvocationTargetException |
                             IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                };
                break;
            } catch (ClassNotFoundException e) {
                // ignore
            } catch (NoSuchMethodException e) {
                throw new RuntimeException(e);
            }
        }
        available = b;
        func = f;
    }

    @Override
    public ScheduledExecutorService createSingleThreadExecutor(@Nullable ThreadFactory threadFactory) {
        return func.apply(threadFactory);
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean available() {
        return available;
    }
}
