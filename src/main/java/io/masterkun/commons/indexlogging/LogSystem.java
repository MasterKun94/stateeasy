package io.masterkun.commons.indexlogging;

import io.masterkun.commons.indexlogging.impl.EventLoggerImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A logging system that manages and provides {@link EventLogger} instances based on provided configurations.
 * It ensures that each logger is uniquely identified by its configuration, serializer, and optional reader executor.
 * The system also supports metrics registration for monitoring purposes.
 */
public final class LogSystem implements HasMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(LogSystem.class);
    private static final AtomicInteger INSTANCE_ADDER = new AtomicInteger();
    private final int instanceId = INSTANCE_ADDER.getAndIncrement();
    private final Map<String, LoggerHolder> cache = new ConcurrentHashMap<>();
    private final ExecutorPool executorPool;
    private volatile MetricRegister register;
    private volatile boolean shutdown;

    public LogSystem(int threadNumPerDisk) {
        AtomicInteger adder = new AtomicInteger();
        this.executorPool = new ExecutorPool(threadNumPerDisk, r -> {
            Thread thread = new Thread(r);
            thread.setName("LogSystemExecutor-" + instanceId + "-" + adder.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        });
    }

    /**
     * Retrieves or creates an {@link EventLogger} for the given configuration and serializer.
     *
     * @param <T>        the type of the objects to be logged
     * @param config     the configuration for the logger
     * @param serializer the serializer for converting objects to a byte stream
     * @return an {@link EventLogger} instance configured with the provided parameters
     * @throws IOException              if an I/O error occurs during the creation of the logger
     * @throws IllegalArgumentException if a logger with different parameters already exists for the given configuration
     */
    public <T> EventLogger<T> get(LogConfig config,
                                  Serializer<T> serializer) throws IOException {
        return get(config, serializer, null);
    }

    /**
     * Retrieves or creates an {@link EventLogger} for the given configuration, serializer, and optional reader executor.
     *
     * @param <T>            the type of the objects to be logged
     * @param config         the configuration for the logger
     * @param serializer     the serializer for converting objects to a byte stream
     * @param readerExecutor the executor for reading log entries, may be null
     * @return an {@link EventLogger} instance configured with the provided parameters
     * @throws IOException              if an I/O error occurs during the creation of the logger
     * @throws IllegalArgumentException if a logger with different parameters already exists for the given configuration
     */
    public <T> EventLogger<T> get(LogConfig config,
                                  Serializer<T> serializer,
                                  @Nullable Executor readerExecutor) throws IOException {
        if (shutdown) {
            throw new IllegalArgumentException("system already closed");
        }
        try {
            //noinspection unchecked
            return (EventLogger<T>) cache.compute(config.name(), (key, holder) -> {
                if (holder == null) {
                    try {
                        EventLogger<?> logger = new EventLoggerImpl<>(config.name(), config,
                                serializer, executorPool.getOrCreate(config.logDir()),
                                readerExecutor);
                        if (register != null) {
                            register.register(logger);
                        }
                        return new LoggerHolder(logger, config, serializer, readerExecutor);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                } else if (holder.match(config, serializer, readerExecutor)) {
                    return holder;
                } else {
                    throw new IllegalArgumentException("Logger already exist and parameters not match");
                }
            }).logger();
        } catch (RuntimeException e) {
            if (e.getCause() instanceof IOException ioe) {
                throw ioe;
            } else {
                throw e;
            }
        }
    }


    /**
     * Registers the metrics of this component and its associated loggers with the given MeterRegistry.
     *
     * <p>This method allows the component to expose its metrics, such as counters, gauges, and timers,
     * to a monitoring system. The metrics are registered with the specified prefix and additional tags.
     * It also adds two additional tags: "log_system_id" and the instance ID of the current LogSystem.
     * This method calls the overloaded {@code register} method with executor metrics disabled.
     *
     * @param metricPrefix the prefix to be used for all metrics registered by this component
     * @param registry     the MeterRegistry to which the metrics will be registered
     * @param tags         optional tags to be associated with the metrics
     */
    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        if (shutdown) {
            throw new IllegalArgumentException("system already closed");
        }
        register(metricPrefix, registry, false);
    }

    /**
     * Registers the metrics of this component and its associated loggers with the given MeterRegistry.
     *
     * <p>This method allows the component to expose its metrics, such as counters, gauges, and timers,
     * to a monitoring system. The metrics are registered with the specified prefix and additional tags.
     * It also adds two additional tags: "log_system_id" and the instance ID of the current LogSystem.
     * If executorMetricsEnabled is true, it also registers metrics for the executor pool.
     *
     * @param metricPrefix           the prefix to be used for all metrics registered by this component
     * @param registry               the MeterRegistry to which the metrics will be registered
     * @param executorMetricsEnabled a flag indicating whether to register metrics for the executor pool
     * @param tags                   optional tags to be associated with the metrics
     */
    public void register(String metricPrefix, MeterRegistry registry, boolean executorMetricsEnabled, String... tags) {
        String[] newTags = Arrays.copyOf(tags, tags.length + 2);
        newTags[tags.length - 2] = "log_system_id";
        newTags[tags.length - 1] = Integer.toString(instanceId);
        synchronized (this) {
            register = logger -> {
                if (logger instanceof HasMetrics hasMetrics) {
                    hasMetrics.register(metricPrefix, registry, newTags.clone());
                }
            };
            for (LoggerHolder holder : cache.values()) {
                register.register(holder.logger);
            }
        }
        if (executorMetricsEnabled) {
            executorPool.register(metricPrefix, registry, newTags);
        }
    }

    public CompletableFuture<Void> shutdown() {
        shutdown = true;
        for (LoggerHolder value : cache.values()) {
            try {
                value.close();
            } catch (Exception e) {
                LOG.error("Error while shutdown logger: {}", value, e);
            }
        }
        cache.clear();
        return executorPool.shutdown();
    }

    public boolean isShutdown() {
        return shutdown;
    }

    private interface MetricRegister {
        void register(EventLogger<?> logger);
    }

    private record LoggerHolder(EventLogger<?> logger,
                                LogConfig config,
                                Serializer<?> serializer,
                                Executor readerExecutor) implements Closeable {
        boolean match(LogConfig config,
                      Serializer<?> serializer,
                      Executor readerExecutor) {
            return Objects.equals(config(), config) &&
                    Objects.equals(serializer(), serializer) &&
                    Objects.equals(readerExecutor(), readerExecutor);
        }

        @Override
        public void close() throws IOException {
            if (logger instanceof Closeable closeable) {
                closeable.close();
            }
        }
    }
}
