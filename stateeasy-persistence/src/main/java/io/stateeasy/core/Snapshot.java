package io.stateeasy.core;

import java.util.Map;

/**
 * A record that represents a snapshot of the state, including the current state, the event ID at
 * which the snapshot was taken, and additional metadata.
 *
 * @param <STATE> the type of the state being managed
 */
public record Snapshot<STATE>(STATE state, long eventId, Map<String, String> metadata) {
}
