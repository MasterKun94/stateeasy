package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.indexlogging.LogSystem;

public class LocalLogEventStoreFactory {
    private final LogSystem logSystem;

    public LocalLogEventStoreFactory(int threadNumPerDisk) {
        this.logSystem = new LogSystem(threadNumPerDisk);
    }
}
