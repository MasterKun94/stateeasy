package io.stateeasy.indexlogging.impl;

import io.stateeasy.indexlogging.exception.LogCorruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

final class MappedByteBufferLogIndexer implements LogIndexer {
    private static final Logger LOG = LoggerFactory.getLogger(MappedByteBufferLogIndexer.class);
    private static final int LEN_64 = 8;
    private static final int LEN_32 = 4;
    private final MappedByteBuffer buffer;
    private int endId;
    private int endOffset;
    private int persistAt;

    MappedByteBufferLogIndexer(MappedByteBuffer buffer,
                               LogState state) throws LogCorruptException {
        this.buffer = buffer;
        int pos = buffer.position();
        switch (state) {
            case INIT:
                if (pos != 0) {
                    throw new LogCorruptException("index file not empty");
                }
                endId = -1;
                endOffset = -1;
                persistAt = 0;
                break;
            case WRITING:
                if (pos % LEN_64 != 0) {
                    pos -= (pos % LEN_64);
                    buffer.position(pos);
                }
                if (pos == 0) {
                    endId = -1;
                    endOffset = -1;
                    persistAt = 0;
                    break;
                }
            case COMPLETE:
                if (pos < LEN_64) {
                    throw new LogCorruptException("get lastId failed");
                }
                if (pos % LEN_64 != 0) {
                    throw new LogCorruptException("index file length check failed");
                }
                endId = buffer.getInt(pos - LEN_64);
                endOffset = buffer.getInt(pos - LEN_32);
                persistAt = buffer.position();
                break;
        }
    }

    public static MappedByteBufferLogIndexer create(File file, int sizeLimit) throws IOException {
        var buffer = Utils.create(file, sizeLimit);
        return new MappedByteBufferLogIndexer(buffer, LogState.INIT);
    }

    public static MappedByteBufferLogIndexer recover(File file, int sizeLimit, boolean readOnly) throws IOException {
        var buffer = Utils.createExistence(file, sizeLimit, readOnly);
        int prevId = 0, prevOff = 0;
        for (int i = 0; i < sizeLimit; i += 8) {
            int id = buffer.getInt(i);
            int off = buffer.getInt(i + 4);
            if (id == 0) {
                if (off != 0) {
                    throw new LogCorruptException("illegal file content");
                }
                buffer.position(i);
                LOG.info("Recover {} index at position {}, with id={}, off={}", file, i, id, off);
                break;
            }
            if (id < prevId) {
                throw new LogCorruptException("id not greater than last at position: " + i);
            }
            if (off < prevOff) {
                throw new LogCorruptException("offset not greater than last at position: " + i);
            }
            prevId = id;
            prevOff = off;
        }
        return new MappedByteBufferLogIndexer(buffer, readOnly ?
                LogState.COMPLETE : LogState.WRITING);
    }

    @Override
    public void append(int id, int offset) {
        assert id > 0;
        if (id <= endId) {
            throw new IllegalArgumentException("id must be greater then last");
        }
        if (offset <= endOffset) {
            throw new IllegalArgumentException("position must be greater then last");
        }
        buffer.putLong(((long) id << 32) | (offset & 0xFFFFFFFFL));
        endId = id;
        endOffset = offset;
    }

    public void persist() {
        int pos = buffer.position();
        int len = pos - persistAt;
        assert len > 0;
        if (LOG.isTraceEnabled()) {
            LOG.trace("Indexer sync from: {} to: {}", persistAt, pos);
        }
        buffer.force(persistAt, len);
        persistAt = pos;
    }

    @Override
    public int offsetBefore(int id) {
        if (endId == -1) {
            return -1;
        }
        if (id == 0) {
            return 0;
        }
        ByteBuffer buffer = this.buffer;
        if (id >= endId) {
            return endOffset;
        }
        int offRight = buffer.position() >>> 3;
        int offLeft = 0;
        while (offLeft < offRight) {
            int off = (offLeft + offRight) >>> 1;
            int pos = off << 3;
            int i = buffer.getInt(pos);
            if (i == id) {
                return buffer.getInt(pos + 4);
            } else if (i > id) {
                if (offLeft == 0 && offRight == 1) {
                    return 0;
                }
                assert offLeft + 1 != offRight;
                offRight = off;
            } else if (offLeft + 1 == offRight) {
                return buffer.getInt((offLeft << 3) + 4);
            } else {
                offLeft = off;
            }

        }
        return buffer.getInt((offLeft << 3) + 4);
    }

    @Override
    public int endId() {
        return endId;
    }

    @Override
    public int endOffset() {
        return endOffset;
    }

    @Override
    public boolean isEmpty() {
        return endId == -1;
    }

    public MappedByteBuffer getBuffer() {
        return buffer;
    }
}
