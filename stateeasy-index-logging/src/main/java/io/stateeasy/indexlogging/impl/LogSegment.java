package io.stateeasy.indexlogging.impl;

import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.indexlogging.LogConfig;
import io.stateeasy.indexlogging.LogIterator;
import io.stateeasy.indexlogging.Serializer;

import java.io.IOException;

/**
 * Represents a segment of a log, which is a part of a larger log system. Each segment is
 * responsible for storing and managing a portion of the log data.
 *
 * <p>This interface provides methods to interact with the log segment, including writing, reading,
 * and managing the state of the segment.
 *
 * @param <T> the type of the log entries
 */
public interface LogSegment<T> {


    /**
     * Creates a new log segment with the specified configuration and initial parameters.
     *
     * @param <T>        the type of the log entries
     * @param config     the configuration for the log segment
     * @param initId     the initial ID for the log segment
     * @param initOffset the initial offset for the log segment
     * @param executor   the event executor to handle asynchronous tasks
     * @param serializer the serializer to serialize and deserialize log entries
     * @return a new instance of {@link LogSegmentImpl} initialized with the provided parameters
     * @throws IOException if an I/O error occurs during the creation process
     */
    static <T> LogSegmentImpl<T> create(LogConfig config, long initId, long initOffset,
                                        EventExecutor executor,
                                        Serializer<T> serializer) throws IOException {
        return LogSegmentImpl.create(config, initId, initOffset, executor, serializer);
    }

    /**
     * Recovers a log segment from the provided configuration and initializes it for use.
     *
     * @param <T>        the type of the log entries
     * @param config     the configuration for the log segment
     * @param initId     the initial ID to start the recovery process
     * @param executor   the event executor to handle asynchronous tasks
     * @param serializer the serializer to serialize and deserialize log entries
     * @param readOnly   whether the recovered log segment should be in read-only mode
     * @return the recovered log segment
     * @throws IOException if an I/O error occurs during the recovery process
     */
    static <T> LogSegmentImpl<T> recover(LogConfig config, long initId,
                                         EventExecutor executor, Serializer<T> serializer,
                                         boolean readOnly) throws IOException {
        return LogSegmentImpl.recover(config, initId, executor, serializer, readOnly);
    }

    long startId();

    long endId();

    long nextId();

    long startOffset();

    long endOffset();

    long nextOffset();

    Writer<T> getWriter();

    Reader<T> getReader();

    void setReadOnly() throws IOException;

    boolean isReadOnly();

    void delete();

    interface Writer<T> {

        boolean put(T value, Callback callback, boolean flush);

        void flush();

        void addListener(long startFromId, long timeoutMills, LogWriter.ReadListener listener);
    }

    interface Reader<T> {

        LogIterator<T> get(long offsetAfter, long id, int limit);
    }
}
