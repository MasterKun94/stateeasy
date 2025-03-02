package io.masterkun.commons.indexlogging;

import io.masterkun.commons.indexlogging.executor.ExecutorFactory;
import io.micrometer.core.instrument.ImmutableTag;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
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
 * A pool of {@link ScheduledExecutorService} instances, each associated with a specific file store.
 * This class ensures that a fixed number of single-threaded executors are available for each file store,
 * and provides methods to get or create these executors. It also supports registering metrics for monitoring.
 */
public class ExecutorPool implements HasMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(ExecutorPool.class);

    private final Map<String, ExecutorHolder> executors = new ConcurrentHashMap<>();
    private final int threadNumPerDisk;
    private final ExecutorFactory executorFactory;
    private final ThreadFactory threadFactory;
    private MetricsWrapper wrapper;
    private volatile CompletableFuture<Void> shutdownFuture;

    public ExecutorPool(int threadNumPerDisk,
                        @Nullable ThreadFactory threadFactory) {
        this(threadNumPerDisk, threadFactory, ExecutorFactory.DEFAULT);
    }

    public ExecutorPool(int threadNumPerDisk,
                        @Nullable ThreadFactory threadFactory,
                        ExecutorFactory executorFactory) {
        this.threadNumPerDisk = threadNumPerDisk;
        this.executorFactory = executorFactory;
        this.threadFactory = threadFactory;
    }

    /**
     * Retrieves or creates a {@link ScheduledExecutorService} for the given directory.
     * <p>
     * This method ensures that there is a dedicated executor service for the file store
     * associated with the provided directory. If the maximum number of threads per disk
     * has not been reached, a new executor service is created and added to the pool.
     * Otherwise, an existing executor from the pool is returned.
     *
     * @param dir the directory for which to get or create the executor service
     * @return a {@link ScheduledExecutorService} for the specified directory
     * @throws RuntimeException if an I/O error occurs while retrieving the file store
     */
    public ScheduledExecutorService getOrCreate(File dir) {
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
            return ExecutorServiceMetrics.monitor(registry, executor,
                    "log_system_executor", metricPrefix, tagList);
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
        ScheduledExecutorService wrap(ScheduledExecutorService executor, String fileStore, int id);
    }

    private final class ExecutorHolder {
        private final String fileStoreName;
        private final List<ScheduledExecutorService> list = new ArrayList<>();
        private final AtomicInteger adder = new AtomicInteger();
        private final AtomicBoolean metricsRegistered = new AtomicBoolean();
        private volatile boolean shutdown;

        private ExecutorHolder(String fileStoreName) {
            this.fileStoreName = fileStoreName;
        }

        public ScheduledExecutorService nextExecutor() {
            synchronized (this) {
                if (shutdown) {
                    throw new RuntimeException("already shutdown");
                }
                if (list.size() < threadNumPerDisk) {
                    var executor = executorFactory.createSingleThreadExecutor(threadFactory);
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
