package io.masterkun.stateeasy.indexlogging.impl;

public interface Callback {
    void onAppend(long id, long offset);

    void onPersist();

    void onError(Throwable e);
}
