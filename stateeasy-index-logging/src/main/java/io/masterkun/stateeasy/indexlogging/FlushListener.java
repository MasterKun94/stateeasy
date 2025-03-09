package io.masterkun.stateeasy.indexlogging;

import io.masterkun.stateeasy.indexlogging.IdAndOffset;

public interface FlushListener {
    void onReceive(IdAndOffset idAndOffset);

    void onError(Throwable e);
}
