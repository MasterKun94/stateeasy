package io.stateeasy.concurrent;

import io.micrometer.core.instrument.MeterRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Interface for components that can register metrics with a {@link MeterRegistry}. Implementing
 * this interface allows the component to expose its metrics, such as counters, gauges, and timers,
 * to a monitoring system.
 */
public interface HasMetrics {

    /**
     * Registers the metrics of this component and its associated loggers with the given
     * MeterRegistry.
     *
     * <p>This method allows the component to expose its metrics, such as counters, gauges, and
     * timers, to a monitoring system. The metrics are registered with the specified prefix and
     * additional tags. It also adds two additional tags: "log_system_id" and the instance ID of the
     * current LogSystem.
     *
     * @param metricPrefix the prefix to be used for all metrics registered by this component, may
     *                     be null
     * @param registry     the MeterRegistry to which the metrics will be registered
     * @param tags         optional tags to be associated with the metrics
     */
    void register(@Nullable String metricPrefix, MeterRegistry registry, String... tags);
}
