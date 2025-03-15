package io.masterkun.stateeasy.core;

import java.time.Duration;

/**
 * Configuration class for managing state snapshots. This class allows for the customization of
 * snapshot-related settings, such as the interval at which snapshots are taken, the maximum number
 * of messages that can be processed before a snapshot is triggered, and whether snapshots should
 * automatically expire.
 */
public class SnapshotConfig {
    /**
     * Defines the interval at which state snapshots are taken. This duration specifies how
     * frequently the system will automatically create a snapshot of the current state, allowing for
     * periodic backups and recovery points.
     * <p>
     * The default value is set to 10 seconds, but it can be adjusted according to the specific
     * requirements of the application.
     */
    private Duration snapshotInterval = Duration.ofSeconds(10);
    /**
     * The maximum number of messages that can be processed before a snapshot is automatically
     * triggered. This setting helps to ensure that the state is periodically saved, even if the
     * time-based {@code snapshotInterval} has not yet elapsed. When the number of processed
     * messages reaches this threshold, a snapshot will be taken.
     */
    private int snapshotMsgMax = 1000;
    /**
     * Indicates whether the snapshots should automatically expire after a certain period or
     * condition. When set to {@code true}, the system will automatically manage and remove old
     * snapshots based on the configured expiration policy. If set to {@code false}, snapshots will
     * not be automatically expired, and manual management may be required.
     */
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
