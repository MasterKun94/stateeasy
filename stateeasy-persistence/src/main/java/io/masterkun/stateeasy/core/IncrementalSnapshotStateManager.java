package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;

public class IncrementalSnapshotStateManager<STATE, EVENT>
        extends AbstractStateManager<STATE, EVENT, IncrementalSnapshotStateDef<STATE, EVENT>> {
    private final boolean lazyMerge;
    private STATE fullState;

    IncrementalSnapshotStateManager(EventExecutor executor,
                                    IncrementalSnapshotStateDef<STATE, EVENT> stateDef) {
        super(executor, stateDef);
        lazyMerge = stateDef.incrementalSnapshotConfig().lazyMerge();
    }

    @Override
    protected void afterStart() {
        fullState = state;
        state = stateDef.initialState();
    }

    @Override
    protected STATE internalGetState() {
        if (lazyMerge) {
            return stateDef.merge(fullState, state);
        }
        return fullState;
    }

    @Override
    protected void internalUpdate(EVENT event) {
        state = stateDef.update(state, event);
        if (lazyMerge) {
            return;
        }
        fullState = stateDef.update(fullState, event);
    }
}
