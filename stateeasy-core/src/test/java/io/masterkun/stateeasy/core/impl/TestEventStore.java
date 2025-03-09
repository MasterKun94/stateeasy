package io.masterkun.stateeasy.core.impl;

public class TestEventStore<EVENT> extends MemoryEventStore<EVENT> {
    public TestEventStore(int capacity) {
        super(capacity);
    }
}
