package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.impl.SnapshotAndId;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;

public interface StateStore<STATE> extends Closeable {

    void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener);

    void write(Snapshot<STATE> snapshot, EventStageListener<Long> listener);

    void read(EventStageListener<@Nullable SnapshotAndId<STATE>> listener);

    void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener);

    @Override
    void close();
}
