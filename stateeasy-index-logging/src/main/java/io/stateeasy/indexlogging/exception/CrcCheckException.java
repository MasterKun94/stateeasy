package io.stateeasy.indexlogging.exception;

public class CrcCheckException extends RuntimeException {
    public CrcCheckException(int id, int offset) {
        super("CRC check failed for id:" + id + ", offset: " + offset);
    }
}
