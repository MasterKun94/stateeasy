package io.stateeasy.indexlogging;

/**
 * A record class that encapsulates an ID and an offset, typically used to represent the position of
 * a log entry. This class is immutable and provides a convenient way to pass around and manipulate
 * the ID and offset together.
 *
 * <p>The {@code IdAndOffset} class is primarily used in logging systems where each log entry has a
 * unique ID and an offset within a log segment. The ID uniquely identifies the log entry, while the
 * offset represents the position within the log segment.
 *
 * @param id     the unique identifier for the log entry
 * @param offset the position of the log entry within a log segment
 */
public record IdAndOffset(long id, long offset) {
}
