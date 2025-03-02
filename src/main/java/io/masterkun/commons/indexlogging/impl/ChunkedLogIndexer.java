package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.HasMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class ChunkedLogIndexer implements LogIndexer, HasMetrics {

    private final MappedByteBufferLogIndexer indexer;
    private final ScheduledExecutorService executor;
    private final int chunkSize;
    private final int persistSize;
    private final long persistIntervalNs;
    private int currentId;
    private int currentOffset;
    private int lastPersistOffset;
    private ScheduledFuture<?> persistTask;
    private Timer syncTimer;

    ChunkedLogIndexer(MappedByteBufferLogIndexer indexer, ScheduledExecutorService executor,
                      int chunkSize, int persistSize, Duration persistInterval) {
        this.indexer = indexer;
        this.executor = executor;
        this.chunkSize = chunkSize;
        this.currentId = indexer.endId();
        this.currentOffset = indexer.endOffset();
        this.persistSize = persistSize;
        this.persistIntervalNs = persistInterval.toNanos();
        this.lastPersistOffset = currentOffset;
    }

    @Override
    public void update(int id, int offset) {
        assert currentId < id;
        assert currentOffset < offset;
        this.currentId = id;
        this.currentOffset = offset;
        if (offset - indexer.endOffset() > chunkSize) {
            indexer.update(id, offset);
            if (offset - lastPersistOffset > persistSize) {
                if (persistTask != null) {
                    persistTask.cancel(false);
                }
                sync();
            } else if (persistTask == null) {
                executor.schedule(this::sync, persistIntervalNs, TimeUnit.NANOSECONDS);
            }
        }
    }

    private void sync() {
        persistTask = null;
        if (syncTimer == null) {
            indexer.persist();
        } else {
            syncTimer.record(indexer::persist);
        }
        lastPersistOffset = currentOffset;
    }

    @Override
    public int offsetBefore(int id) {
        return id >= currentId ? currentOffset : indexer.offsetBefore(id);
    }

    @Override
    public int endId() {
        return currentId;
    }

    @Override
    public int endOffset() {
        return currentOffset;
    }

    @Override
    public boolean isEmpty() {
        return indexer.isEmpty();
    }

    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        String name = Utils.metricName(metricPrefix, "event.logger.indexer.sync");
        syncTimer = registry.timer(name, tags);
    }
}
