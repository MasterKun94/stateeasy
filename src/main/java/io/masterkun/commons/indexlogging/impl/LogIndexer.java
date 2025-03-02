package io.masterkun.commons.indexlogging.impl;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

public sealed interface LogIndexer permits ChunkedLogIndexer, MappedByteBufferLogIndexer {
    static LogIndexer create(File file, int sizeLimit, int chunkSize, int persistSize,
                             Duration persistInterval,
                             ScheduledExecutorService executor) throws IOException {
        var inner = MappedByteBufferLogIndexer.create(file, sizeLimit);
        return new ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    static LogIndexer recover(File file, int sizeLimit, int chunkSize, int persistSize,
                              Duration persistInterval, ScheduledExecutorService executor,
                              boolean readOnly) throws IOException {
        var inner = MappedByteBufferLogIndexer.recover(file, sizeLimit, readOnly);
        return new ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    void update(int id, int position);

    int offsetBefore(int id);

    int endId();

    int endOffset();

    boolean isEmpty();
}
