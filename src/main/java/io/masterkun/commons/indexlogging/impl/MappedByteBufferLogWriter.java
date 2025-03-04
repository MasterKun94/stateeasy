package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.HasMetrics;
import io.masterkun.commons.indexlogging.Serializer;
import io.masterkun.commons.indexlogging.exception.LogFullException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

final class MappedByteBufferLogWriter implements LogWriter, HasMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(MappedByteBufferLogWriter.class);

    private final MappedByteBuffer buffer;
    private final LogReader reader;
    private final LogIndexer indexer;
    private final ScheduledExecutorService executor;
    private final int autoFlushSize;
    private final long autoFlushIntervalNs;
    private final LogDataOut reuseOut;
    private final List<Listener> writeListeners = new ArrayList<>();
    private final List<Listener> readListeners = new ArrayList<>();
    private int lastFlushOffset;
    private int writeId;
    private int writeOffset;
    private ScheduledFuture<?> flushTask;
    private Timer syncTimer;
    private Counter eventsWrite;

    MappedByteBufferLogWriter(LogReader reader, LogIndexer indexer,
                              ScheduledExecutorService executor,
                              int autoFlushSize, Duration autoFlushInterval,
                              boolean serializeBufferDirect, int serializeBufferSizeInit, int serializeBufferSizeMax) {
        this.buffer = ((MappedByteBufferLogReader) reader).getBufferForWrite();
        this.reader = reader;
        this.indexer = indexer;
        this.executor = executor;
        this.autoFlushSize = autoFlushSize;
        this.autoFlushIntervalNs = autoFlushInterval.toNanos();
        this.reuseOut = new LogDataOut(serializeBufferDirect, serializeBufferSizeInit, serializeBufferSizeMax);
        this.writeId = indexer.endId();
        this.writeOffset = indexer.endOffset();
    }

    @Override
    public Serializer.DataOut open(boolean immediateFlush, WriteListener callback) {
        return reuseOut.reset(immediateFlush, callback);
    }

    @Override
    public void flush() {
        sync();
    }

    @Override
    public void addListener(int startFromId, long timeoutMills, ReadListener listener) {
        if (indexer.endId() >= startFromId) {
            listener.onFlush();
        } else {
            readListeners.add(new ListenerWithTimeout(timeoutMills, listener));
        }
    }

    @Override
    public void close() {
        if (flushTask != null) {
            flushTask.cancel(false);
        }
        sync();
    }

    private void sync() {
        if (syncTimer == null) {
            doSync();
        } else {
            syncTimer.record(this::doSync);
        }
    }

    private void doSync() {
        flushTask = null;
        int pos = buffer.position();
        int len = pos - lastFlushOffset;
        if (len > 0) {
            if (LOG.isTraceEnabled()) {
                LOG.trace("Writer sync from: {} to: {}", lastFlushOffset, pos);
            }
            buffer.force(lastFlushOffset, len);
            lastFlushOffset = pos;
            reader.setReadable(pos);
            for (Listener listener : writeListeners) {
                try {
                    listener.onFlush();
                } catch (Exception e) {
                    LOG.error("Unexpected WriteListener.onFlush error", e);
                }
            }
            writeListeners.clear();
            if (!readListeners.isEmpty()) {
                for (Listener listener : readListeners) {
                    try {
                        listener.onFlush();
                    } catch (Exception e) {
                        LOG.error("Unexpected ReadListener.onFlush error", e);
                    }
                }
                readListeners.clear();
            }
            assert writeId >= 0;
            assert writeOffset >= 0;
            indexer.update(writeId, writeOffset);
        }
    }

    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        String timerTime = Utils.metricName(metricPrefix, "event.logger.writer.sync");
        syncTimer = registry.timer(timerTime, tags);
        String counterName = Utils.metricName(metricPrefix, "event.logger.writer.events.write");
        eventsWrite = registry.counter(counterName, tags);
    }

    private class LogDataOut extends ByteBufferDataOutputStream {
        private boolean immediateFlush;
        private WriteListener listener;

        public LogDataOut(boolean direct, int initialCapacity, int maxCapacity) {
            super(direct, initialCapacity, maxCapacity);
        }

        public LogDataOut reset(boolean immediateFlush, WriteListener callback) {
            this.immediateFlush = immediateFlush;
            this.listener = Objects.requireNonNull(callback);
            getBuffer().clear().position(8);
            return this;
        }

        @Override
        public void close() throws LogFullException {
            if (listener == null) {
                throw new IllegalArgumentException("close again");
            }
            try {
                ByteBuffer serializeBuffer = getBuffer();
                MappedByteBuffer mappedBuffer = MappedByteBufferLogWriter.this.buffer;
                if (mappedBuffer.remaining() < serializeBuffer.position() + 4) {
                    throw LogFullException.INSTANCE;
                }
                int id = writeId + 1;
                int len = serializeBuffer.position() - 8;
                serializeBuffer.putInt(Utils.crc(id, len));
                serializeBuffer.putInt(0, id);
                serializeBuffer.putInt(4, len);
                int pos = mappedBuffer.position();
                mappedBuffer.put(serializeBuffer.flip());
                writeListeners.add(listener);
                listener.onAppend(id, pos);
                writeId = id;
                writeOffset = pos;
                if (immediateFlush || mappedBuffer.position() - lastFlushOffset > autoFlushSize) {
                    if (flushTask != null) {
                        flushTask.cancel(false);
                    }
                    sync();
                } else if (flushTask == null) {
                    flushTask = executor.schedule(MappedByteBufferLogWriter.this::sync,
                            autoFlushIntervalNs, TimeUnit.NANOSECONDS);
                }
            } finally {
                listener = null;
                if (eventsWrite != null) {
                    eventsWrite.increment();
                }
            }
        }

        @Override
        public void flush() {
            // no nothing
        }
    }

    private class ListenerWithTimeout implements Listener {

        private final ScheduledFuture<?> f;
        private final ReadListener listener;

        private ListenerWithTimeout(long timeoutMills, ReadListener listener) {
            this.listener = listener;
            f = executor.schedule(() -> {
                readListeners.remove(this);
                this.listener.onTimeout();
            }, timeoutMills, TimeUnit.MILLISECONDS);
        }

        @Override
        public void onFlush() {
            f.cancel(false);
            listener.onFlush();
        }
    }
}
