package io.masterkun.commons.indexlogging.exception;

public class LogFullException extends RuntimeException {
    public static final LogFullException INSTANCE = new LogFullException();

    private LogFullException() {
        setStackTrace(new StackTraceElement[0]);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
