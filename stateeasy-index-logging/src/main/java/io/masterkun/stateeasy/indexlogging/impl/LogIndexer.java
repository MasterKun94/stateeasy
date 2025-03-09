package io.masterkun.stateeasy.indexlogging.impl;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

public sealed interface LogIndexer permits io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer, io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer {
    static LogIndexer create(File file, int sizeLimit, int chunkSize, int persistSize,
                             Duration persistInterval,
                             ScheduledExecutorService executor) throws IOException {
        var inner = io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.create(file, sizeLimit);
        return new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    static LogIndexer recover(File file, int sizeLimit, int chunkSize, int persistSize,
                              Duration persistInterval, ScheduledExecutorService executor,
                              boolean readOnly) throws IOException {
        var inner = io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.recover(file, sizeLimit, readOnly);
        return new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    void update(int id, int position);

    int offsetBefore(int id);

    int endId();

    int endOffset();

    boolean isEmpty();
}
