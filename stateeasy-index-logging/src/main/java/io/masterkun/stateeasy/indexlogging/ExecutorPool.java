package io.masterkun.stateeasy.indexlogging;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutorThreadFactory;
import io.masterkun.stateeasy.indexlogging.executor.EventExecutorFactory;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * A pool of {@link EventExecutor} instances, each associated with a specific file store. The pool
 * is designed to manage and provide executors for different file stores, ensuring that the number
 * of threads per disk does not exceed a specified limit.
 *
 * <p>The class implements the {@link HasMetrics} interface, allowing it to register metrics for
 * monitoring purposes.
 */
public class ExecutorPool implements HasMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorPool.class);

    private final Map<String, ExecutorHolder> executors = new ConcurrentHashMap<>();
    private final int threadNumPerDisk;
    private final EventExecutorFactory executorFactory;
    private final EventExecutorThreadFactory threadFactory;
    private MetricsWrapper wrapper;
    private volatile CompletableFuture<Void> shutdownFuture;

    /**
     *
     */
    public ExecutorPool(int threadNumPerDisk,
                        @Nullable EventExecutorThreadFactory threadFactory) {
        this(threadNumPerDisk, threadFactory, EventExecutorFactory.DEFAULT);
    }

    /**
     * Constructs a new ExecutorPool with the specified number of threads per disk, thread factory,
     * and executor factory.
     *
     * @param threadNumPerDisk the number of threads to allocate per disk
     * @param threadFactory    the factory to use for creating new threads, or null to use the
     *                         default factory
     * @param executorFactory  the factory to use for creating new event executors
     */
    public ExecutorPool(int threadNumPerDisk,
                        @Nullable EventExecutorThreadFactory threadFactory,
                        EventExecutorFactory executorFactory) {
        this.threadNumPerDisk = threadNumPerDisk;
        this.executorFactory = executorFactory;
        this.threadFactory = threadFactory;
    }

    /**
     * Retrieves or creates an {@link EventExecutor} for the specified directory.
     * <p>
     * This method ensures that there is a dedicated executor service for the file store associated
     * with the provided directory. If the maximum number of threads per disk has not been reached,
     * a new executor service is created and added to the pool. Otherwise, an existing executor from
     * the pool is returned.
     *
     * @param dir the directory for which to retrieve or create an {@code EventExecutor}
     * @return the existing or newly created {@code EventExecutor} associated with the file store of
     * the given directory
     * @throws RuntimeException if the executor pool has already been shut down, or if there is an
     *                          issue accessing the file store
     */
    public EventExecutor getOrCreate(File dir) {
        if (shutdownFuture != null) {
            throw new RuntimeException("already shutdown");
        }
        File parent = dir;
        while (!parent.exists()) {
            parent = parent.getParentFile();
        }
        FileStore fileStore;
        try {
            fileStore = Files.getFileStore(dir.toPath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return executors.computeIfAbsent(fileStore.name(), ExecutorHolder::new).nextExecutor();
    }

    private void checkIsSingleThread(ScheduledExecutorService executor) {
        if (executor instanceof ScheduledThreadPoolExecutor e) {
            if (e.getMaximumPoolSize() > 1) {
                throw new IllegalArgumentException("executor is not single thread");
            }
        }
    }

    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        if (shutdownFuture != null) {
            throw new RuntimeException("already shutdown");
        }
        wrapper = (executor, fileStore, id) -> {
            List<Tag> tagList = new ArrayList<>();
            for (int i = 0; i < tags.length; i += 2) {
                tagList.add(new ImmutableTag(tags[i], tags[i + 1]));
            }
            tagList.add(new ImmutableTag("file_store", fileStore));
            tagList.add(new ImmutableTag("executor_id", Integer.toString(id)));
            // TODO add metrics
//            return ExecutorServiceMetrics.monitor(registry, (ScheduledExecutorService) executor,
//                    "log_system_executor", metricPrefix, tagList);
            return executor;
        };
        for (ExecutorHolder holder : executors.values()) {
            holder.registerMetrics();
        }
    }

    public CompletableFuture<Void> shutdown() {
        shutdownFuture = new CompletableFuture<>();
        for (ExecutorHolder value : executors.values()) {
            value.shutdown();
        }
        new Thread(() -> {
            try {
                for (ExecutorHolder value : executors.values()) {
                    value.awaitTermination();
                }
                shutdownFuture.complete(null);
            } catch (Exception e) {
                shutdownFuture.completeExceptionally(e);
            }
        }).start();
        return shutdownFuture;
    }

    private interface MetricsWrapper {
        EventExecutor wrap(EventExecutor executor, String fileStore, int id);
    }

    private final class ExecutorHolder {
        private final String fileStoreName;
        private final List<EventExecutor> list = new ArrayList<>();
        private final AtomicInteger adder = new AtomicInteger();
        private final AtomicBoolean metricsRegistered = new AtomicBoolean();
        private volatile boolean shutdown;

        private ExecutorHolder(String fileStoreName) {
            this.fileStoreName = fileStoreName;
        }

        public EventExecutor nextExecutor() {
            synchronized (this) {
                if (shutdown) {
                    throw new RuntimeException("already shutdown");
                }
                if (list.size() < threadNumPerDisk) {
                    EventExecutor executor = executorFactory.createEventExecutor(threadFactory);
                    if (wrapper != null) {
                        executor = wrapper.wrap(executor, fileStoreName, list.size());
                    }
                    checkIsSingleThread(executor);
                    list.add(executor);
                    return executor;
                }
                return list.get(adder.getAndIncrement() % threadNumPerDisk);
            }
        }

        public void registerMetrics() {
            synchronized (this) {
                if (shutdown) {
                    throw new RuntimeException("already shutdown");
                }
                if (metricsRegistered.compareAndSet(false, true)) {
                    for (int id = 0; id < list.size(); id++) {
                        list.set(id, wrapper.wrap(list.get(id), fileStoreName, id));
                    }
                }
            }

        }

        public void shutdown() {
            synchronized (this) {
                shutdown = true;
                for (ScheduledExecutorService executor : list) {
                    executor.shutdown();
                }
            }
        }

        public void awaitTermination() {
            for (ExecutorService executor : list) {
                try {
                    executor.awaitTermination(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    LOG.warn("Thread interrupt", e);
                }
            }
        }
    }
}
