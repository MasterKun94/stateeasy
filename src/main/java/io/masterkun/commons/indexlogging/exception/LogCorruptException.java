package io.masterkun.commons.indexlogging.exception;

import java.io.IOException;

public class LogCorruptException extends IOException {
    public LogCorruptException(String message) {
        super(message);
    }
}
