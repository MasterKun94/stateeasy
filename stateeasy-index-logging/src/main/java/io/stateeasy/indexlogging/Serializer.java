package io.stateeasy.indexlogging;

import java.io.Closeable;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A generic interface for serializing and deserializing objects of type T.
 * <p>
 * Implementations of this interface provide the necessary logic to convert objects of a specific
 * type into a byte stream (serialization) and to reconstruct objects from a byte stream
 * (deserialization).
 *
 * @param <T> the type of the object that can be serialized and deserialized
 */
public interface Serializer<T> {
    /**
     * Serializes the given object to the specified output stream.
     *
     * @param obj the object to be serialized
     * @param out the output stream to which the serialized data will be written
     * @throws IOException if an I/O error occurs during the serialization process
     */
    void serialize(T obj, DataOut out) throws IOException;

    /**
     * Deserializes an object from the given input stream.
     *
     * @param in the input stream to read the serialized data from
     * @return the deserialized object
     * @throws IOException if an I/O error occurs during the deserialization process
     */
    T deserialize(DataIn in) throws IOException;

    interface DataIn extends DataInput {
        /**
         * Reads all available data from the input source into the specified {@link ByteBuffer}.
         *
         * @param buf the buffer to read data into
         * @throws IOException if an I/O error occurs during the read operation
         */
        void readAll(ByteBuffer buf) throws IOException;

        /**
         * Reads data from the input source and fills the specified {@link ByteBuffer} until it is
         * full.
         *
         * @param buf the buffer to read data into
         * @throws IOException if an I/O error occurs during the read operation
         */
        void readFully(ByteBuffer buf) throws IOException;

        /**
         * Reads data from the input source and fills the specified portion of the
         * {@link ByteBuffer} until the given length is reached.
         *
         * @param buf the buffer to read data into
         * @param off the offset in the buffer where the data should be placed
         * @param len the number of bytes to read
         * @throws IOException if an I/O error occurs during the read operation
         */
        void readFully(ByteBuffer buf, int off, int len) throws IOException;

        /**
         * Returns the number of bytes that can be read from this input source.
         *
         * @return the number of bytes that can be read
         * @throws IOException if an I/O error occurs
         */
        int available() throws IOException;
    }

    interface DataOut extends DataOutput, Closeable {
        /**
         * Writes the content of the specified {@link ByteBuffer} to the output source.
         *
         * @param buf the {@link ByteBuffer} containing the data to be written
         * @throws IOException if an I/O error occurs during the write operation
         */
        void write(ByteBuffer buf) throws IOException;

        /**
         * Writes a portion of the specified {@link ByteBuffer} to the output source, starting from
         * the given offset and writing up to the specified length.
         *
         * @param buf the {@link ByteBuffer} containing the data to be written
         * @param off the offset in the buffer from which to start writing
         * @param len the number of bytes to write
         * @throws IOException if an I/O error occurs during the write operation
         */
        void write(ByteBuffer buf, int off, int len) throws IOException;

        @Override
        void close();
    }
}
