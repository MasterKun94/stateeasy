package io.stateeasy.indexlogging.impl;

import java.util.Iterator;
import java.util.NoSuchElementException;

public interface LogSegmentIterator<T> extends Iterator<LogSegmentRecord<T>> {
    static <T> LogSegmentIterator<T> empty(int id, int offset) {
        return new LogSegmentIterator<>() {

            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public LogSegmentRecord<T> next() {
                throw new NoSuchElementException();
            }

            @Override
            public int nextId() {
                return id;
            }

            @Override
            public int nextOffset() {
                return offset;
            }
        };
    }

    int nextId();

    int nextOffset();
}
