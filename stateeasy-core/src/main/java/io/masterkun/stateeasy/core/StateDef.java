package io.masterkun.stateeasy.core;

import java.util.concurrent.ScheduledExecutorService;

public interface StateDef<STATE, EVENT> {

    SnapshotConfig snapshotConfig();

    STATE initialState();

    STATE update(STATE state, EVENT msg);

    StateStore<STATE> stateStore(ScheduledExecutorService executor);
}
