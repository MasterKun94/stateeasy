package io.masterkun.stateeasy.indexlogging.impl;

public record LogEntry<T>(long id, long offset, T value) {

}
