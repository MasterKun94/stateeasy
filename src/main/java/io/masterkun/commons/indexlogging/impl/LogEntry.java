package io.masterkun.commons.indexlogging.impl;

public record LogEntry<T>(long id, long offset, T value) {

}
