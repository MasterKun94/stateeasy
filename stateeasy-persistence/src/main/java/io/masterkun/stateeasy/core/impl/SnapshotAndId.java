package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.Snapshot;

import java.util.Map;

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
