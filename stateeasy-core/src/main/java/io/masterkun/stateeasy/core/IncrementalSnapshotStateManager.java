package io.masterkun.stateeasy.core;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

public class IncrementalSnapshotStateManager<STATE, EVENT>
        extends AbstractStateManager<STATE, EVENT, IncrementalSnapshotStateDef<STATE, EVENT>> {
    private final boolean lazyMerge;
    private STATE fullState;

    IncrementalSnapshotStateManager(ScheduledExecutorService singleThreadExecutor,
                                    IncrementalSnapshotStateDef<STATE, EVENT> stateDef) {
        super(singleThreadExecutor, stateDef);
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

    @Override
    public <T> CompletableFuture<T> query(Function<STATE, T> function) {
        return super.query(function);
    }

    @Override
    public <T> CompletableFuture<T> queryFast(Function<STATE, T> function) {
        return super.queryFast(function);
    }
}
