package io.masterkun.stateeasy.indexlogging;

import io.masterkun.stateeasy.indexlogging.impl.LogSegmentRecord;

import java.util.Objects;

/**
 * Represents a log record that encapsulates an ID, offset, and a value. This class is used to
 * manage log entries in a logging system, providing methods to compute the final ID and offset of
 * the log entry.
 *
 * <p>The {@code LogRecord} class is designed to be immutable, with the ID and offset being
 * computed based on initial values and the values from the encapsulated {@link LogSegmentRecord}.
 *
 * @param <T> the type of the value stored in the log record
 */
public class LogRecord<T> {
    private final long initId;
    private final long initOffset;
    private final LogSegmentRecord<T> record;

    public LogRecord(long initId, long initOffset, LogSegmentRecord<T> record) {
        this.initId = initId;
        this.initOffset = initOffset;
        this.record = record;
    }

    public IdAndOffset idAndOffset() {
        return new IdAndOffset(id(), offset());
    }

    public long id() {
        return initId + record.id();
    }

    public long offset() {
        return initOffset + record.offset();
    }

    public T value() {
        return record.value();
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        LogRecord<?> logRecord = (LogRecord<?>) o;
        return initId == logRecord.initId && initOffset == logRecord.initOffset && Objects.equals(record, logRecord.record);
    }

    @Override
    public int hashCode() {
        return Objects.hash(initId, initOffset, record);
    }

    @Override
    public String toString() {
        return "LogRecord[" +
                "id=" + id() +
                ", offset=" + offset() +
                ", value=" + value() +
                ']';
    }
}
