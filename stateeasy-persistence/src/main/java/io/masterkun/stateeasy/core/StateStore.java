package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventStageListener;

import java.io.Closeable;

public interface StateStore<STATE> extends Closeable {

    void initialize(StateDef<STATE, ?> stateDef, EventStageListener<Void> listener);

    void write(Snapshot<STATE> snapshot, EventStageListener<Void> listener);

    void read(EventStageListener<Snapshot<STATE>> listener);

    void expire(long expireBeforeSnapshotId, EventStageListener<Boolean> listener);

    @Override
    void close();
}
