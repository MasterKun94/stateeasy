package io.masterkun.stateeasy.indexlogging.impl;

import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class ByteBufferDataInputOutputStreamTest {

    @Test
    public void test() throws Exception {
        ByteBufferDataOutputStream dout = new ByteBufferDataOutputStream(true, 16, 4096);
        dout.write(1);
        dout.write(new byte[]{1, 2, 3, 4});
        dout.write(new byte[]{1, 2, 3, 4, 5});
        dout.write(new byte[]{1, 2, 3, 4, 5, 6});
        dout.writeByte(3);
        dout.writeBoolean(true);
        dout.writeShort(5);
        dout.writeInt(10);
        dout.writeLong(11);
        dout.writeFloat(12.5f);
        dout.writeDouble(12.6d);
        dout.writeChar('a');
        dout.writeUTF("hello");
        dout.write(ByteBuffer.wrap(new byte[]{5, 2, 3, 1}), 1, 2);
        dout.write(ByteBuffer.wrap(new byte[]{5, 6, 7, 8}), 0, 4);
        dout.write(ByteBuffer.wrap(new byte[]{6, 7, 8, 9}));
        ByteBuffer buffer = dout.getBuffer();
        ByteBufferDataInputStream din = new ByteBufferDataInputStream(buffer.flip());
        Assert.assertEquals(1, din.read());
        byte[] buf = new byte[4];
        Assert.assertEquals(4, din.read(buf));
        Assert.assertArrayEquals(new byte[]{1, 2, 3, 4}, buf);
        buf = new byte[5];
        din.readFully(buf);
        Assert.assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, buf);
        buf = new byte[6];
        Assert.assertEquals(6, din.read(buf));
        Assert.assertArrayEquals(new byte[]{1, 2, 3, 4, 5, 6}, buf);
        Assert.assertEquals(3, din.readByte());
        Assert.assertTrue(din.readBoolean());
        Assert.assertEquals(5, din.readShort());
        Assert.assertEquals(10, din.readInt());
        Assert.assertEquals(11, din.readLong());
        Assert.assertEquals(12.5f, din.readFloat(), 0.001);
        Assert.assertEquals(12.6d, din.readDouble(), 0.001);
        Assert.assertEquals('a', din.readChar());
        Assert.assertEquals("hello", din.readUTF());
        ByteBuffer allocate = ByteBuffer.allocate(10).limit(2);
        din.readFully(allocate);
        buf = Arrays.copyOfRange(allocate.array(), allocate.arrayOffset(), allocate.position());
        Assert.assertArrayEquals(new byte[]{2, 3}, buf);
        allocate.limit(10).position(0);
        din.readFully(allocate, 4, 4);
        buf = new byte[4];
        allocate.get(4, buf, 0, 4);
        Assert.assertArrayEquals(new byte[]{5, 6, 7, 8}, buf);
        allocate.clear();
        din.readAll(allocate);
        allocate.get(0, buf, 0, 4);
        Assert.assertEquals(4, allocate.position());
        Assert.assertArrayEquals(new byte[]{6, 7, 8, 9}, buf);

        Assert.assertFalse(buffer.hasRemaining());
        Assert.assertEquals(-1, din.read());
        Assert.assertEquals(-1, din.read(buf, 0, 3));
    }
}
