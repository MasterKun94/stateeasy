package io.stateeasy.indexlogging.impl;

import io.stateeasy.concurrent.EventExecutor;

import java.io.File;
import java.io.IOException;
import java.time.Duration;

/**
 * A sealed interface for managing and indexing log data. This interface provides methods for
 * updating, retrieving, and checking the state of a log index. It is designed to be implemented by
 * classes that handle specific log indexing strategies.
 *
 * <p>The {@code LogIndexer} interface is sealed, allowing only the specified implementations:
 * {@code ChunkedLogIndexer} and {@code MappedByteBufferLogIndexer}.
 */
public sealed interface LogIndexer permits ChunkedLogIndexer, MappedByteBufferLogIndexer {


    /**
     * Creates a new instance of {@code LogIndexer} using the specified parameters.
     *
     * @param file            the file to use for storing the log index
     * @param sizeLimit       the maximum size limit for the log index
     * @param chunkSize       the size of each chunk in the log index
     * @param persistSize     the size at which to persist the log index
     * @param persistInterval the interval at which to persist the log index
     * @param executor        the event executor to use for asynchronous tasks
     * @return a new instance of {@code LogIndexer}
     * @throws IOException if an I/O error occurs while creating the log indexer
     */
    static LogIndexer create(File file, int sizeLimit, int chunkSize, int persistSize,
                             Duration persistInterval,
                             EventExecutor executor) throws IOException {
        var inner = MappedByteBufferLogIndexer.create(file, sizeLimit);
        return new ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    /**
     * Recovers a {@code LogIndexer} from a given file.
     *
     * @param file            the file to use for recovering the log index
     * @param sizeLimit       the maximum size limit for the log index
     * @param chunkSize       the size of each chunk in the log index
     * @param persistSize     the size at which to persist the log index
     * @param persistInterval the interval at which to persist the log index
     * @param executor        the event executor to use for asynchronous tasks
     * @param readOnly        if true, the recovered log indexer will be read-only
     * @return a new instance of {@code LogIndexer} that has been recovered from the specified file
     * @throws IOException if an I/O error occurs while recovering the log indexer
     */
    static LogIndexer recover(File file, int sizeLimit, int chunkSize, int persistSize,
                              Duration persistInterval, EventExecutor executor,
                              boolean readOnly) throws IOException {
        var inner = MappedByteBufferLogIndexer.recover(file, sizeLimit, readOnly);
        return new ChunkedLogIndexer(inner, executor, chunkSize, persistSize, persistInterval);
    }

    /**
     * Append an index into the indexer.
     *
     * @param id     the identifier of the log entry
     * @param offset the offset of the log entry
     */
    void append(int id, int offset);

    /**
     * Returns the offset of the log entry with the given identifier, or the offset of the closest
     * preceding log entry if the exact identifier is not found.
     *
     * @param id the identifier of the log entry
     * @return the offset of the log entry with the given identifier or the closest preceding log
     * entry
     */
    int offsetBefore(int id);

    /**
     * Returns the identifier of the last log entry in the indexer.
     *
     * @return the identifier of the last log entry, or -1 if the indexer is empty
     */
    int endId();

    /**
     * Returns the offset of the last log entry in the indexer.
     *
     * @return the offset of the last log entry, or -1 if the indexer is empty
     */
    int endOffset();

    /**
     * Checks if the log indexer is empty.
     *
     * @return {@code true} if the log indexer contains no entries, {@code false} otherwise
     */
    boolean isEmpty();
}
