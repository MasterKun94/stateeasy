package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.File;
import java.io.IOException;

/**
 * A sealed interface for reading log segments from a file. This interface is implemented by
 * {@link MappedByteBufferLogReader} and provides methods for setting the readable offset, getting
 * offsets, and iterating over log segments.
 *
 * <p>This interface includes static factory methods for creating or recovering a log reader
 * instance.
 *
 * @see MappedByteBufferLogReader
 */
sealed interface LogReader permits MappedByteBufferLogReader {
    static MappedByteBufferLogReader create(File file, int sizeLimit) throws IOException {
        return MappedByteBufferLogReader.create(file, sizeLimit);
    }

    /**
     * Recovers a log reader from the given file, using the provided indexer to help in the recovery
     * process.
     *
     * @param file      The file to recover the log reader from.
     * @param indexer   The log indexer to assist with the recovery.
     * @param sizeLimit The maximum size limit for the buffer.
     * @param readOnly  Whether the recovered log should be opened in read-only mode.
     * @return A new instance of MappedByteBufferLogReader initialized with the recovered data.
     * @throws IOException if an I/O error occurs during the recovery process.
     */
    static MappedByteBufferLogReader recover(File file, LogIndexer indexer, int sizeLimit,
                                             boolean readOnly) throws IOException {
        return MappedByteBufferLogReader.create(file, indexer, sizeLimit, readOnly);
    }

    /**
     * Sets the readable offset for the log reader.
     *
     * @param offset the offset to set as the new readable position
     */
    void setReadable(int offset);

    /**
     * Retrieves the offset for the given log segment ID.
     *
     * @param offsetAfter the offset after which to find the target offset
     * @param id          the ID of the log segment
     * @return the offset for the given log segment ID, or -1 if no such offset exists
     */
    int getOffset(int offsetAfter, int id);

    /**
     * Retrieves the next offset after the specified offset for the given log segment ID.
     *
     * @param offsetAfter the offset after which to find the next offset
     * @param id          the ID of the log segment
     * @return the next offset after the specified offset for the given log segment ID, or -1 if no
     * such offset exists
     */
    int getNextOffset(int offsetAfter, int id);

    /**
     * Retrieves a {@link LogSegmentIterator} for the specified log segment.
     *
     * @param offsetAfter the offset after which to start reading the log segment
     * @param id          the ID of the log segment
     * @param limit       the maximum number of records to read
     * @param serializer  the serializer to use for deserializing the log segment records
     * @param <T>         the type of the objects in the log segment
     * @return a {@link LogSegmentIterator} for the specified log segment
     */
    <T> LogSegmentIterator<T> get(int offsetAfter, int id, int limit, Serializer<T> serializer);
}
