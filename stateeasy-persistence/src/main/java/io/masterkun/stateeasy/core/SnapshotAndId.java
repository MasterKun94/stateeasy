package io.masterkun.stateeasy.core;

import java.util.Map;

/**
 * A record that encapsulates a snapshot of the state along with its unique identifier.
 *
 * @param <STATE> the type of the state being managed
 */
public record SnapshotAndId<STATE>(long snapshotId, Snapshot<STATE> snapshot) {
    public STATE state() {
        return snapshot.state();
    }

    public long eventId() {
        return snapshot.eventId();
    }

    public Map<String, String> metadata() {
        return snapshot.metadata();
    }
}
