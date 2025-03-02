package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.Serializer;

import java.io.Closeable;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

sealed interface LogWriter extends Closeable permits MappedByteBufferLogWriter {
    static LogWriter create(LogReader reader, LogIndexer indexer, ScheduledExecutorService executor,
                            int autoFlushSize, Duration autoFlushInterval,
                            boolean serializeBufferDirect, int serializeBufferSizeInit,
                            int serializeBufferSizeMax) {
        return new MappedByteBufferLogWriter(reader, indexer, executor, autoFlushSize,
                autoFlushInterval, serializeBufferDirect, serializeBufferSizeInit, serializeBufferSizeMax);
    }

    Serializer.DataOut open(boolean immediateFlush, WriteListener callback);

    void flush();

    void addListener(int startFromId, long timeoutMills, ReadListener listener);

    @Override
    void close();

    interface Listener {
        void onFlush();
    }

    interface WriteListener extends Listener {
        void onAppend(int id, int offset);
    }

    interface ReadListener extends Listener {
        void onTimeout();
    }
}
