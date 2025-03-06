package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.Serializer;

import java.io.File;
import java.io.IOException;

sealed interface LogReader permits MappedByteBufferLogReader {
    static MappedByteBufferLogReader create(File file, int sizeLimit) throws IOException {
        return MappedByteBufferLogReader.create(file, sizeLimit);
    }

    static MappedByteBufferLogReader recover(File file, LogIndexer indexer, int sizeLimit,
                                             boolean readOnly) throws IOException {
        return MappedByteBufferLogReader.create(file, indexer, sizeLimit, readOnly);
    }

    void setReadable(int offset);

    int getOffset(int offsetAfter, int id);

    int getNextOffset(int offsetAfter, int id);

    <T> LogSegmentIterator<T> get(int offsetAfter, int id, int limit, Serializer<T> serializer);
}
