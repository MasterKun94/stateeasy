package io.masterkun.commons.indexlogging.exception;

public class IdExpiredException extends RuntimeException {
    private final long id;

    public IdExpiredException(long id) {
        super("Id " + id + " is expired");
        this.id = id;
    }

    public long getId() {
        return id;
    }
}
