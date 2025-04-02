package io.stateeasy.indexlogging.impl;

import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.SingleThreadEventExecutor;
import io.stateeasy.indexlogging.LogConfig;
import io.stateeasy.indexlogging.LogIterator;
import io.stateeasy.indexlogging.LogObserver;
import io.stateeasy.indexlogging.LogRecord;
import io.stateeasy.indexlogging.Serializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LogSegmentImplTest {

    private static final File logDir = new File("src/test/resources/.test/testSegment");

    @BeforeClass
    public static void beforeClass() throws IOException {
        if (logDir.exists()) {
            for (File file : Objects.requireNonNull(logDir.listFiles())) {
                file.delete();
            }
        }
        logDir.mkdirs();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        if (logDir.exists()) {
            for (File file : Objects.requireNonNull(logDir.listFiles())) {
                file.delete();
            }
        }
    }

    @Test
    public void testCreate() throws IOException {
        LogConfig config = LogConfig.builder("test", logDir)
                .segmentSize(1024 * 1024)
                .indexChunkSize(1024)
                .indexPersistSize(2048)
                .indexPersistInterval(Duration.ofMillis(20))
                .autoFlushSize(1024)
                .autoFlushInterval(Duration.ofMillis(10))
                .serializeBufferDirect(true)
                .serializeBufferInit(1024)
                .serializeBufferMax(1024 * 1024)
                .build();

        EventExecutor executor = Mockito.mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);
        Serializer<byte[]> serializer = new Serializer<>() {
            @Override
            public void serialize(byte[] obj, DataOut out) throws IOException {
                out.writeInt(obj.length);
                out.write(obj);
            }

            @Override
            public byte[] deserialize(DataIn in) throws IOException {
                byte[] bytes = new byte[in.readInt()];
                in.readFully(bytes);
                return bytes;
            }
        };

        LogSegment<byte[]> logSegment = LogSegmentImpl.create(config, 1L, 2L, executor, serializer);

        assertNotNull(logSegment);
        assertEquals(1, logSegment.startId());
        assertEquals(2, logSegment.startOffset());
        assertEquals(-1, logSegment.endId());
        assertEquals(-1, logSegment.endOffset());
        assertFalse(logSegment.isReadOnly());
        var callback = new TestCallback(1, 2);
        assertTrue(logSegment.getWriter().put(new byte[]{1, 2, 3, 4}, callback, false));
        verify(executor).schedule(any(Runnable.class), eq(10_000_000L), eq(TimeUnit.NANOSECONDS));
        assertEquals(1, logSegment.startId());
        assertEquals(2, logSegment.startOffset());
        assertEquals(-1, logSegment.endId());
        assertEquals(-1, logSegment.endOffset());
        assertFalse(callback.flushed);
        logSegment.getWriter().flush();
        assertEquals(1, logSegment.endId());
        assertEquals(2, logSegment.endOffset());
        assertTrue(callback.flushed);

        byte[] data = new byte[1025];
        data[0] = 12;
        data[1024] = 21;
        callback = new TestCallback(2, 22);
        assertTrue(logSegment.getWriter().put(data, callback, false));
        assertEquals(2, logSegment.endId());
        assertEquals(22, logSegment.endOffset());
        assertTrue(callback.flushed);

        callback = new TestCallback(3, 1063);
        assertTrue(logSegment.getWriter().put(new byte[]{4, 5, 6}, callback, true));
        assertEquals(3, logSegment.endId());
        assertEquals(22 + 1025 + 16, logSegment.endOffset());
        assertTrue(callback.flushed);

        callback = new TestCallback(4, 1082);
        assertTrue(logSegment.getWriter().put(new byte[]{7, 8, 9}, callback, false));
        assertFalse(callback.flushed);
        var callback2 = new TestCallback(5, 1101);
        assertTrue(logSegment.getWriter().put(new byte[]{7, 8, 9}, callback2, true));
        assertTrue(callback.flushed);
        assertTrue(callback2.flushed);

        test(logSegment, 1, new TestObserver(
                new Elem(1, 2, new byte[]{1, 2, 3, 4}),
                new Elem(2, 22, data),
                new Elem(3, 1063, new byte[]{4, 5, 6}),
                new Elem(4, 1082, new byte[]{7, 8, 9}),
                new Elem(5, 1101, new byte[]{7, 8, 9})
        ), 100);
        test(logSegment, 2, new TestObserver(
                new Elem(2, 22, data),
                new Elem(3, 1063, new byte[]{4, 5, 6}),
                new Elem(4, 1082, new byte[]{7, 8, 9}),
                new Elem(5, 1101, new byte[]{7, 8, 9})
        ), 4);
        test(logSegment, 2, new TestObserver(
                new Elem(2, 22, data),
                new Elem(3, 1063, new byte[]{4, 5, 6})
        ), 2);
    }

    private void test(LogSegment<byte[]> segment, int id, LogObserver<byte[]> observer, int limit) {
        LogIterator<byte[]> iter = segment.getReader().get(-1, id, limit);
        while (iter.hasNext()) {
            LogRecord<byte[]> next = iter.next();
            observer.onNext(next.id(), next.offset(), next.value());
        }
        observer.onComplete(iter.nextId(), iter.nextOffset());
    }

    private static class TestCallback implements Callback {
        private final long id;
        private final long offset;
        private boolean flushed;

        private TestCallback(long id, long offset) {
            this.id = id;
            this.offset = offset;
        }

        @Override
        public void onAppend(long id, long offset) {
            Assert.assertEquals(this.id, id);
            Assert.assertEquals(this.offset, offset);
        }

        @Override
        public void onPersist() {
            flushed = true;
        }

        @Override
        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }
    }

    private record Elem(long id, long offset, byte[] data) {
    }

    private class TestObserver implements LogObserver<byte[]> {
        private final Queue<Elem> queue;
        private long nextId;
        private long nextOffset;

        private TestObserver(Elem... elems) {
            this.queue = new LinkedList<>(Arrays.asList(elems));
        }

        @Override
        public void onNext(long id, long offset, byte[] data) {
            Elem poll = queue.poll();
            Assert.assertNotNull(poll);
            Assert.assertEquals(poll.id, id);
            Assert.assertEquals(poll.offset, offset);
            Assert.assertArrayEquals(data, poll.data);
            nextId = id + 1;
            nextOffset = offset + poll.data.length + 16;
        }

        @Override
        public void onComplete(long nextId, long nextOffset) {
            if (nextId > 0) {
                Assert.assertEquals(nextId, this.nextId);
                Assert.assertEquals(nextOffset, this.nextOffset);
            }
        }

        @Override
        public void onError(Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
