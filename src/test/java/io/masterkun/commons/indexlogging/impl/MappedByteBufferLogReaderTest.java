package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.Serializer;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

public class MappedByteBufferLogReaderTest {

    private static final File testFile = new File("src/test/resources/.test/reader.log");

    @BeforeClass
    public static void beforeClass() throws IOException {
        if (testFile.exists()) {
            testFile.delete();
        }
        testFile.getParentFile().mkdirs();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        testFile.delete();
    }

    @Test
    public void test() throws IOException {
        MappedByteBufferLogReader reader = MappedByteBufferLogReader.create(testFile, 1024 * 1024);
        MappedByteBuffer bufferForWrite = reader.getBufferForWrite();
        var out = new ByteBufferDataOutputStream(bufferForWrite);
        out.writeInt(5);
        out.writeInt(4);
        out.write(new byte[]{1, 2, 3, 4});
        out.writeInt(Utils.crc(5, 4));

        out.writeInt(6);
        out.writeInt(5);
        out.write(new byte[]{2, 3, 4, 5, 6});
        out.writeInt(Utils.crc(6, 5));

        out.writeInt(7);
        out.writeInt(3);
        out.write(new byte[]{4, 5, 6});
        out.writeInt(Utils.crc(7, 3));

        out.flush();
        Assert.assertEquals(36 + 4 + 5 + 3, bufferForWrite.position());
        bufferForWrite.force();
        reader.setReadable(bufferForWrite.position());

        test(reader, 0, 5, new TestDrainer(
                new Elem(5, 0, new byte[]{1, 2, 3, 4}),
                new Elem(6, 16, new byte[]{2, 3, 4, 5, 6}),
                new Elem(7, 33, new byte[]{4, 5, 6})
        ), 10);

        test(reader, 0, 5, new TestDrainer(
                new Elem(5, 0, new byte[]{1, 2, 3, 4}),
                new Elem(6, 16, new byte[]{2, 3, 4, 5, 6})
        ), 2);

        test(reader, 0, 5, new TestDrainer(
                new Elem(5, 0, new byte[]{1, 2, 3, 4})
        ), 1);

        test(reader, 16, 6, new TestDrainer(
                new Elem(6, 16, new byte[]{2, 3, 4, 5, 6}),
                new Elem(7, 33, new byte[]{4, 5, 6})
        ), 10);

        test(reader, 16, 6, new TestDrainer(
                new Elem(6, 16, new byte[]{2, 3, 4, 5, 6})
        ), 1);
        test(reader, 0, 7, new TestDrainer(
                new Elem(7, 33, new byte[]{4, 5, 6})
        ), 10);
        test(reader, 16, 7, new TestDrainer(
                new Elem(7, 33, new byte[]{4, 5, 6})
        ), 10);
        test(reader, 33, 7, new TestDrainer(
                new Elem(7, 33, new byte[]{4, 5, 6})
        ), 10);

        test(reader, 33 + 15, 8, new TestDrainer(), 10);
    }

    private void test(LogReader reader, int offset, int id, TestDrainer drainer, int limit) throws IOException {
        LogSegmentIterator<byte[]> iter = reader.get(offset, id, limit, new Serializer<>() {
            @Override
            public void serialize(byte[] obj, DataOut out) {
                throw new IllegalArgumentException();
            }

            @Override
            public byte[] deserialize(DataIn in) throws IOException {
                byte[] bytes = new byte[in.available()];
                in.readFully(bytes);
                return bytes;
            }
        });
        while (iter.hasNext()) {
            LogSegmentRecord<byte[]> next = iter.next();
            drainer.drainTo(next.id(), next.offset(), next.value());
        }
        drainer.end(iter.nextId(), iter.nextOffset());
    }

    private record Elem(int id, int offset, byte[] data) {
    }

    private class TestDrainer {
        private final Queue<Elem> queue;
        private int nextId;
        private int nextOffset;

        private TestDrainer(Elem... elems) {
            this.queue = new LinkedList<>(Arrays.asList(elems));
        }

        public void drainTo(int id, int offset, byte[] in) throws IOException {
            Elem poll = queue.poll();
            Assert.assertNotNull(poll);
            Assert.assertEquals(poll.id, id);
            Assert.assertEquals(poll.offset, offset);
            Assert.assertArrayEquals(in, poll.data);
            nextId = id + 1;
            nextOffset = offset + poll.data.length + 12;
        }

        public void end(int nextId, int nextOffset) {
            if (this.nextId > 0) {
                Assert.assertEquals(nextId, this.nextId);
                Assert.assertEquals(nextOffset, this.nextOffset);
            }
        }
    }
}
