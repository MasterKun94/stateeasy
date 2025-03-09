package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.HasMetrics;
import io.masterkun.stateeasy.indexlogging.Serializer;
import io.masterkun.stateeasy.indexlogging.exception.CrcCheckException;
import io.masterkun.stateeasy.indexlogging.impl.ByteBufferDataInputStream;
import io.masterkun.stateeasy.indexlogging.impl.LogIndexer;
import io.masterkun.stateeasy.indexlogging.impl.LogSegmentIterator;
import io.masterkun.stateeasy.indexlogging.impl.LogSegmentRecord;
import io.masterkun.stateeasy.indexlogging.impl.Utils;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;

public final class MappedByteBufferLogReader implements io.masterkun.stateeasy.indexlogging.impl.LogReader, HasMetrics {
    private static final Logger LOG = LoggerFactory.getLogger(MappedByteBufferLogReader.class);

    private static final int HEAD_LEN = 12;
    private static final int EMPTY = -1;
    private final MappedByteBuffer buffer;
    private volatile int readable;
    private Counter eventsRead;

    public MappedByteBufferLogReader(MappedByteBuffer buffer) {
        this.buffer = buffer;
        readable = buffer.position();
    }

    public static MappedByteBufferLogReader create(File file, int sizeLimit) throws IOException {
        var buffer = io.masterkun.stateeasy.indexlogging.impl.Utils.create(file, sizeLimit);
        return new MappedByteBufferLogReader(buffer);
    }

    public static MappedByteBufferLogReader create(File file, LogIndexer indexer, int sizeLimit, boolean readOnly) throws IOException {
        var buffer = io.masterkun.stateeasy.indexlogging.impl.Utils.createExistence(file, sizeLimit, readOnly);
        int id;
        int off;
        if (indexer.isEmpty()) {
            id = 0;
            off = 0;
        } else {
            int len = HEAD_LEN + getLenAndCheckCrc(indexer.endId(), indexer.endOffset(), buffer);
            id = indexer.endId() + 1;
            off = indexer.endOffset() + len;
        }
        assert id >= 0;
        assert off >= 0;
        while (buffer.capacity() - off > 12) {
            int id0 = buffer.getInt(off);
            if (id0 != id) {
                break;
            }
            int len;
            try {
                len = getLenAndCheckCrc(id, off, buffer);
            } catch (CrcCheckException e) {
                LOG.warn("File {} crc check failed while recover", file, e);
                break;
            }
            if (indexer.endId() < id) {
                indexer.update(id, off);
            }
            id++;
            off += HEAD_LEN + len;
        }
        LOG.info("File {} recover at offset: {}, id: {}", file, off, id);
        buffer.position(off);
        return new MappedByteBufferLogReader(buffer);
    }

    private static int getLenAndCheckCrc(int id, int offset, MappedByteBuffer buffer) throws CrcCheckException {
        int len = buffer.getInt(offset + 4);
        if (io.masterkun.stateeasy.indexlogging.impl.Utils.crc(id, len) != buffer.getInt(offset + 8 + len)) {
            throw new CrcCheckException(id, offset);
        }
        return len;
    }

    private Serializer.DataIn dataInput(int offset, int len, io.masterkun.stateeasy.indexlogging.impl.ByteBufferDataInputStream reuse) {
        MappedByteBuffer slice = buffer.slice(offset + 8, len);
        if (reuse == null) {
            return new io.masterkun.stateeasy.indexlogging.impl.ByteBufferDataInputStream(slice);
        } else {
            reuse.setBuffer(slice);
            return reuse;
        }
    }

    @Override
    public void setReadable(int offset) {
        buffer.position(offset);
        readable = offset;
    }

    @Override
    public int getOffset(int offsetAfter, int id) throws CrcCheckException {
        MappedByteBuffer buffer = this.buffer;
        int readablePos = readable;
        if (offsetAfter >= readablePos) {
            return EMPTY;
        }
        int i;
        int off = offsetAfter;
        while ((i = buffer.getInt(off)) < id) {
            off += HEAD_LEN + getLenAndCheckCrc(i, off, buffer);
            if (off >= readablePos) {
                return EMPTY;
            }
        }
        if (i > id) {
            if (offsetAfter == off) {
                return EMPTY;
            }
            throw new RuntimeException("should never happen");
        }
        return off;
    }

    @Override
    public int getNextOffset(int offsetAfter, int id) {
        int off = getOffset(offsetAfter, id);
        if (off == EMPTY) {
            return EMPTY;
        }
        off += HEAD_LEN + getLenAndCheckCrc(id, off, buffer);
        return off;
    }

    @Override
    public <T> io.masterkun.stateeasy.indexlogging.impl.LogSegmentIterator<T> get(int offsetAfter, int id, int limit, Serializer<T> serializer) {
        var offsetAfter0 = getOffset(offsetAfter, id);
        if (offsetAfter0 == EMPTY) {
            return io.masterkun.stateeasy.indexlogging.impl.LogSegmentIterator.empty(id, offsetAfter0);
        }
        return new DataIterator<>(serializer, limit, offsetAfter0, id);
    }

    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        String name = Utils.metricName(metricPrefix, "event.logger.reader.events.read");
        eventsRead = registry.counter(name, tags);
    }

    MappedByteBuffer getBufferForWrite() {
        MappedByteBuffer buffer = this.buffer.slice(0, this.buffer.capacity());
        buffer.position(readable);
        return buffer;
    }

    private class DataIterator<T> implements LogSegmentIterator<T> {
        private final io.masterkun.stateeasy.indexlogging.impl.ByteBufferDataInputStream reuse = new ByteBufferDataInputStream(null);
        private final Serializer<T> serializer;
        private final int limitId;
        private final int readablePos = readable;
        private int offsetAfter;
        private int id;

        private DataIterator(Serializer<T> serializer, int limit, int offsetAfter, int id) {
            this.serializer = serializer;
            this.limitId = id + limit;
            this.offsetAfter = offsetAfter;
            this.id = id;
        }

        @Override
        public boolean hasNext() {
            return id < limitId && offsetAfter < readablePos;
        }

        @Override
        public LogSegmentRecord<T> next() {
            int id = buffer.getInt(offsetAfter);
            assert this.id == id;
            int len = getLenAndCheckCrc(id, offsetAfter, buffer);
            try {
                T obj = serializer.deserialize(dataInput(offsetAfter, len, reuse));
                return new LogSegmentRecord<>(id, offsetAfter, obj);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                offsetAfter += HEAD_LEN + len;
                this.id++;
                if (eventsRead != null) {
                    eventsRead.increment();
                }
            }
        }

        @Override
        public int nextId() {
            return id;
        }

        @Override
        public int nextOffset() {
            return offsetAfter;
        }
    }
}
