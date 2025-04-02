package io.stateeasy.indexlogging.impl;

import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.indexlogging.Serializer;

import java.io.Closeable;
import java.time.Duration;

/**
 * A sealed interface for writing log segments to a file. This interface is implemented by
 * {@link MappedByteBufferLogWriter} and provides methods for opening a data output, flushing the
 * buffer, adding listeners, and closing the writer.
 *
 * <p>This interface includes a static factory method for creating a new instance of a log writer.
 *
 * @see MappedByteBufferLogWriter
 */
sealed interface LogWriter extends Closeable permits MappedByteBufferLogWriter {
    /**
     * Creates a new instance of a log writer.
     *
     * @param reader                  the log reader to be used for reading log segments
     * @param indexer                 the log indexer to be used for indexing log segments
     * @param executor                the event executor to handle asynchronous tasks
     * @param autoFlushSize           the size in bytes at which the buffer should be automatically
     *                                flushed
     * @param autoFlushInterval       the interval at which the buffer should be automatically
     *                                flushed
     * @param serializeBufferDirect   whether the serialization buffer should be direct or not
     * @param serializeBufferSizeInit the initial size of the serialization buffer
     * @param serializeBufferSizeMax  the maximum size of the serialization buffer
     * @return a new instance of {@link MappedByteBufferLogWriter}
     */
    static LogWriter create(LogReader reader, LogIndexer indexer, EventExecutor executor,
                            int autoFlushSize, Duration autoFlushInterval,
                            boolean serializeBufferDirect, int serializeBufferSizeInit,
                            int serializeBufferSizeMax) {
        return new MappedByteBufferLogWriter(reader, indexer, executor, autoFlushSize,
                autoFlushInterval, serializeBufferDirect, serializeBufferSizeInit,
                serializeBufferSizeMax);
    }

    /**
     * Opens a new data output stream for writing log segments.
     *
     * @param immediateFlush if true, the buffer will be flushed immediately after the DataOut is
     *                       closed; otherwise, it will be flushed according to the auto-flush
     *                       settings
     * @param callback       the write listener to be notified when data is appended to the log
     * @return a {@link Serializer.DataOut} object that can be used to write data to the log
     */
    Serializer.DataOut open(boolean immediateFlush, WriteListener callback);

    /**
     * Forces any buffered data to be written to the underlying storage. This method ensures that
     * all data currently in the buffer is flushed and made persistent, regardless of the auto-flush
     * settings.
     */
    void flush();

    /**
     * Adds a listener to be notified of read timeouts.
     *
     * @param startFromId  the starting ID from which the listener should be active
     * @param timeoutMills the timeout duration in milliseconds after which the listener's onTimeout
     *                     method will be called
     * @param listener     the ReadListener to be added
     */
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
