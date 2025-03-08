package io.masterkun.commons.indexlogging;

public interface FlushListener {
    void onReceive(IdAndOffset idAndOffset);

    void onError(Throwable e);
}
