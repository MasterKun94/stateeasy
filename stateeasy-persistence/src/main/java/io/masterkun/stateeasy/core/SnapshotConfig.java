package io.masterkun.stateeasy.core;

import java.time.Duration;

public record SnapshotConfig(
        Duration snapshotInterval,
        boolean autoExpire
) {
}
