package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.LogConfig;
import io.masterkun.stateeasy.indexlogging.LogIterator;
import io.masterkun.stateeasy.indexlogging.Serializer;
import io.masterkun.stateeasy.indexlogging.impl.Callback;
import io.masterkun.stateeasy.indexlogging.impl.LogSegmentImpl;

import java.io.IOException;
import java.util.concurrent.ScheduledExecutorService;

public interface LogSegment<T> {
    static <T> LogSegmentImpl<T> create(LogConfig config, long initId, long initOffset,
                                        ScheduledExecutorService executor,
                                        Serializer<T> serializer) throws IOException {
        return LogSegmentImpl.create(config, initId, initOffset, executor, serializer);
    }

    static <T> LogSegmentImpl<T> recover(LogConfig config, long initId,
                                         ScheduledExecutorService executor,
                                         Serializer<T> serializer,
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

        void addListener(long startFromId, long timeoutMills, io.masterkun.stateeasy.indexlogging.impl.LogWriter.ReadListener listener);
    }

    interface Reader<T> {

        LogIterator<T> get(long offsetAfter, long id, int limit);
    }
}
