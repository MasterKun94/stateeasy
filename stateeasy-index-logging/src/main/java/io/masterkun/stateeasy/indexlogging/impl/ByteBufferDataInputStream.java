package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * A specialized input stream that reads data from a {@link ByteBuffer} and implements the
 * {@link Serializer.DataIn} interface. This class provides methods to read various types of data
 * (such as bytes, shorts, ints, longs, floats, doubles, booleans, and strings) from the underlying
 * byte buffer. It also supports reading into other buffers and skipping bytes.
 */
public class ByteBufferDataInputStream extends InputStream implements Serializer.DataIn {
    private ByteBuffer buffer;

    public ByteBufferDataInputStream(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    public void setBuffer(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    private boolean notReadable(int size) {
        return buffer.remaining() < size;
    }

    @Override
    public int read() {
        if (notReadable(1)) return -1;
        return buffer.get() & 0xFF;
    }

    @Override
    public int read(byte[] bytes, int offset, int length) {
        if (length == 0) return 0;
        int count = Math.min(buffer.remaining(), length);
        if (count == 0) return -1;
        buffer.get(bytes, offset, count);
        return count;
    }

    @Override
    public int available() {
        return buffer.remaining();
    }

    @Override
    public void readFully(byte[] b) throws EOFException {
        readFully(b, 0, b.length);
    }

    @Override
    public void readFully(byte[] b, int off, int len) throws EOFException {
        if (notReadable(len)) throw new EOFException();
        buffer.get(b, off, len);
    }

    @Override
    public int skipBytes(int n) {
        int count = Math.min(buffer.remaining(), n);
        if (count == 0) return 0;
        buffer.position(buffer.position() + count);
        return count;
    }

    @Override
    public boolean readBoolean() throws EOFException {
        if (notReadable(1)) throw new EOFException();
        return buffer.get() != 0;
    }

    @Override
    public byte readByte() throws EOFException {
        if (notReadable(1)) throw new EOFException();
        return buffer.get();
    }

    @Override
    public int readUnsignedByte() throws EOFException {
        if (notReadable(1)) throw new EOFException();
        return Byte.toUnsignedInt(buffer.get());
    }

    @Override
    public short readShort() throws EOFException {
        if (notReadable(2)) throw new EOFException();
        return buffer.getShort();
    }

    @Override
    public int readUnsignedShort() throws EOFException {
        if (notReadable(2)) throw new EOFException();
        return Short.toUnsignedInt(buffer.getShort());
    }

    @Override
    public char readChar() throws EOFException {
        if (notReadable(2)) throw new EOFException();
        return buffer.getChar();
    }

    @Override
    public int readInt() throws EOFException {
        if (notReadable(4)) throw new EOFException();
        return buffer.getInt();
    }

    @Override
    public long readLong() throws EOFException {
        if (notReadable(8)) throw new EOFException();
        return buffer.getLong();
    }

    @Override
    public float readFloat() throws EOFException {
        if (notReadable(4)) throw new EOFException();
        return buffer.getFloat();
    }

    @Override
    public double readDouble() throws EOFException {
        if (notReadable(8)) throw new EOFException();
        return buffer.getDouble();
    }

    @Deprecated
    @Override
    public String readLine() throws EOFException {
        try {
            return new DataInputStream(this).readLine();
        } catch (EOFException e) {
            throw e;
        } catch (IOException e) {
            throw new EOFException();
        }
    }

    @Override
    public String readUTF() throws EOFException {
        try {
            return DataInputStream.readUTF(this);
        } catch (EOFException e) {
            throw e;
        } catch (IOException e) {
            throw new EOFException();
        }
    }

    @Override
    public void readAll(ByteBuffer buf) {
        buf.put(buffer);
    }

    @Override
    public void readFully(ByteBuffer buf) throws EOFException {
        int limit = buf.limit();
        int position = buf.position();
        readFully(buf, position, limit - position);
        buf.position(limit);
    }

    @Override
    public void readFully(ByteBuffer buf, int off, int len) throws EOFException {
        if (notReadable(len)) throw new EOFException();
        int pos = buffer.position();
        buf.put(off, buffer, pos, len);
        buffer.position(pos + len);
    }

    @Override
    public void close() {
    }
}
