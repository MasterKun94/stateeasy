package io.masterkun.stateeasy.concurrent;

public abstract class EventStageListenerAdaptor<T> implements EventStageListener<T> {
    @Override
    public final void success(T value) {
        complete(value, null);
    }

    @Override
    public final void failure(Throwable cause) {
        complete(null, cause);
    }

    protected abstract void complete(T value, Throwable cause);
}
