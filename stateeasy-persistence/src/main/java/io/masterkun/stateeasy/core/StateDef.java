package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;

public interface StateDef<STATE, EVENT> {
    String name();

    SnapshotConfig snapshotConfig();

    STATE initialState();

    STATE update(STATE state, EVENT msg);

    StateStore<STATE> stateStore(EventExecutor executor);
}
