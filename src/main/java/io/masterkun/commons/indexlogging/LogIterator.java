package io.masterkun.commons.indexlogging;

import io.masterkun.commons.indexlogging.impl.LogSegmentIterator;

import java.util.Iterator;

/**
 * An iterator for traversing log records, providing a way to iterate over a sequence of {@link LogRecord} instances.
 * <p>
 * This class wraps an underlying {@link LogSegmentIterator} and adjusts the IDs and offsets of the log records
 * based on the initial ID and offset provided during construction. The adjusted IDs and offsets are calculated
 * by adding the initial values to the corresponding values from the inner iterator.
 *
 * @param <T> the type of the value contained in the log records
 */
public class LogIterator<T> implements Iterator<LogRecord<T>> {
    private final long initId;
    private final long initOffset;
    private final LogSegmentIterator<T> inner;

    public LogIterator(long initId, long initOffset, LogSegmentIterator<T> inner) {
        this.initId = initId;
        this.initOffset = initOffset;
        this.inner = inner;
    }

    public long nextId() {
        return initId + inner.nextId();
    }

    public long nextOffset() {
        return initOffset + inner.nextOffset();
    }

    @Override
    public boolean hasNext() {
        return inner.hasNext();
    }

    @Override
    public LogRecord<T> next() {
        return new LogRecord<>(initId, initOffset, inner.next());
    }
}
