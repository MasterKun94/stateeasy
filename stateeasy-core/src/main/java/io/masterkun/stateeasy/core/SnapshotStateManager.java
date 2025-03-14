package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;

public class SnapshotStateManager<STATE, EVENT>
        extends AbstractStateManager<STATE, EVENT, SnapshotStateDef<STATE, EVENT>> {

    SnapshotStateManager(EventExecutor executor,
                         SnapshotStateDef<STATE, EVENT> stateDef) {
        super(executor, stateDef);
    }

    @Override
    protected void afterStart() {
    }

    @Override
    protected STATE internalGetState() {
        return state;
    }

    @Override
    protected void internalUpdate(EVENT event) {
        state = stateDef.update(state, event);
    }
}
