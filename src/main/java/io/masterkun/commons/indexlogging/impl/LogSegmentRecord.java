package io.masterkun.commons.indexlogging.impl;

public record LogSegmentRecord<T>(int id, int offset, T value) {
}
