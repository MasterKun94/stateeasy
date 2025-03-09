package io.masterkun.stateeasy.core;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

public interface StateStore<STATE> extends Closeable {
    CompletableFuture<Void> initialize(StateDef<STATE, ?> stateDef);

    CompletableFuture<Void> write(Snapshot<STATE> snapshot);

    CompletableFuture<Snapshot<STATE>> read();

    @Override
    void close();
}
