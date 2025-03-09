package io.masterkun.stateeasy.core;

import java.io.Closeable;
import java.util.Iterator;

public interface RemoteIterator<T> extends Iterator<T>, Closeable {
    @Override
    void close();
}
