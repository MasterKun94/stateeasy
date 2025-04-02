package io.stateeasy.indexlogging.impl;

public record LogSegmentRecord<T>(int id, int offset, T value) {
}
