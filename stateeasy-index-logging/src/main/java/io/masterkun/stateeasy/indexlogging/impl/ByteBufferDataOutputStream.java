package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * A specialized output stream that writes data to a {@link ByteBuffer} with the ability to control
 * whether the buffer is direct or non-direct. This class extends {@link OutputStream} and
 * implements the {@link Serializer.DataOut} interface, providing methods for writing various types
 * of data.
 *
 * <p>The buffer's capacity can be dynamically increased as needed, up to a specified maximum
 * capacity. If the maximum capacity is exceeded, a {@link java.nio.BufferOverflowException} is
 * thrown.
 *
 * <p>This class is useful for scenarios where you need to write data to a byte buffer and later
 * process or send the data in a single chunk.
 */
public class ByteBufferDataOutputStream extends OutputStream implements Serializer.DataOut {
    private final boolean direct;
    private final int maxCapacity;
    private ByteBuffer buffer;
    private DataOutputStream utf8out;

    /**
     * @param direct          if true, the underlying buffer will be a direct byte buffer;
     *                        otherwise, it will be a non-direct buffer
     * @param initialCapacity the initial capacity of the buffer
     * @param maxCapacity     the maximum capacity of the buffer. If 0 or negative, the maximum
     *                        capacity is set to Integer.MAX_VALUE
     */
    public ByteBufferDataOutputStream(boolean direct, int initialCapacity, int maxCapacity) {
        this.direct = direct;
        this.maxCapacity = maxCapacity <= 0 ? Integer.MAX_VALUE : maxCapacity;
        this.buffer = direct ?
                ByteBuffer.allocateDirect(initialCapacity) :
                ByteBuffer.allocate(initialCapacity);
    }

    /**
     * Constructs a new {@code ByteBufferDataOutputStream} that writes data to the specified byte
     * buffer.
     *
     * @param buffer the byte buffer to which data will be written
     */
    public ByteBufferDataOutputStream(ByteBuffer buffer) {
        this.direct = buffer.isDirect();
        this.maxCapacity = buffer.capacity();
        this.buffer = buffer;
    }

    private void ensureCapacity(int required) {
        if (buffer.remaining() < required) {
            ByteBuffer old = buffer;
            int growCapacity = old.capacity() << 1;
            if (maxCapacity <= growCapacity) {
                throw new java.nio.BufferOverflowException();
            }
            buffer = direct ?
                    ByteBuffer.allocateDirect(growCapacity) :
                    ByteBuffer.allocate(growCapacity);
            buffer.put((ByteBuffer) old.flip());
        }
    }

    public void write(int b) {
        ensureCapacity(1);
        buffer.put((byte) b);
    }

    public void write(byte[] bytes, int offset, int length) {
        ensureCapacity(length);
        buffer.put(bytes, offset, length);
    }

    @Override
    public void writeBoolean(boolean v) {
        ensureCapacity(1);
        buffer.put((byte) (v ? 1 : 0));
    }

    @Override
    public void writeByte(int v) {
        ensureCapacity(1);
        buffer.put((byte) v);
    }

    @Override
    public void writeShort(int v) {
        ensureCapacity(2);
        buffer.putShort((short) v);
    }

    @Override
    public void writeChar(int v) {
        ensureCapacity(2);
        buffer.putChar((char) v);
    }

    @Override
    public void writeInt(int v) {
        ensureCapacity(4);
        buffer.putInt(v);
    }

    @Override
    public void writeLong(long v) {
        ensureCapacity(8);
        buffer.putLong(v);
    }

    @Override
    public void writeFloat(float v) {
        ensureCapacity(4);
        buffer.putFloat(v);
    }

    @Override
    public void writeDouble(double v) {
        ensureCapacity(8);
        buffer.putDouble(v);
    }

    @Override
    public void writeBytes(String s) {
        int len = s.length();
        ensureCapacity(len);
        for (int i = 0; i < len; i++) {
            buffer.put((byte) s.charAt(i));
        }
    }

    @Override
    public void writeChars(String s) {
        int len = s.length();
        ensureCapacity(len << 1);
        for (int i = 0; i < len; i++) {
            buffer.putChar(s.charAt(i));
        }
    }

    @Override
    public void writeUTF(String s) {
        DataOutputStream out = utf8out;
        if (out == null) {
            // Suppress a warning since the stream is closed in the close() method
            utf8out = out = new DataOutputStream(this);
        }
        try {
            out.writeUTF(s);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    public ByteBuffer getBuffer() {
        return buffer;
    }

    public void reset() {
        buffer.reset();
    }

    @Override
    public void write(ByteBuffer buf) {
        ensureCapacity(buf.remaining());
        buffer.put(buf);
    }

    @Override
    public void write(ByteBuffer buf, int off, int len) {
        ensureCapacity(len);
        int pos = buffer.position();
        buffer.put(pos, buf, off, len);
        buffer.position(pos + len);
    }

    @Override
    public void close() {
    }
}
