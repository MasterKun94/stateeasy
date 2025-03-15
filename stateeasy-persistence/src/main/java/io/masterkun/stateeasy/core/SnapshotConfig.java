package io.masterkun.stateeasy.core;

import java.time.Duration;

public class SnapshotConfig {
    private Duration snapshotInterval = Duration.ofSeconds(10);
    private int snapshotMsgMax = 1000;
    private boolean autoExpire = true;

    public SnapshotConfig() {
    }

    public Duration getSnapshotInterval() {
        return snapshotInterval;
    }

    public void setSnapshotInterval(Duration snapshotInterval) {
        this.snapshotInterval = snapshotInterval;
    }

    public int getSnapshotMsgMax() {
        return snapshotMsgMax;
    }

    public void setSnapshotMsgMax(int snapshotMsgMax) {
        this.snapshotMsgMax = snapshotMsgMax;
    }

    public boolean isAutoExpire() {
        return autoExpire;
    }

    public void setAutoExpire(boolean autoExpire) {
        this.autoExpire = autoExpire;
    }
}
