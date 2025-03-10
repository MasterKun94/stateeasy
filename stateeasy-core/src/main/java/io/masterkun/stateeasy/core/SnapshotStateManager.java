package io.masterkun.stateeasy.core;

import java.util.concurrent.ScheduledExecutorService;

public class SnapshotStateManager<STATE, EVENT>
        extends AbstractStateManager<STATE, EVENT, SnapshotStateDef<STATE, EVENT>> {

    SnapshotStateManager(ScheduledExecutorService singleThreadExecutor,
                         SnapshotStateDef<STATE, EVENT> stateDef) {
        super(singleThreadExecutor, stateDef);
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
