package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.indexlogging.LogSystem;

public class LocalFileLogSystemProvider {
    private static volatile LogSystem logSystem;

    public static LogSystem getLogSystem(int threadNumPerDisk) {
        if (logSystem == null) {
            synchronized (LocalFileLogSystemProvider.class) {
                if (logSystem == null) {
                    logSystem = new LogSystem(threadNumPerDisk);
                }
            }
        }
        return logSystem;
    }
}
