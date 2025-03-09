package io.masterkun.stateeasy.concurrent;

public interface EventStageListener<T> {
    void success(T value);
    void failure(Throwable cause);
}
