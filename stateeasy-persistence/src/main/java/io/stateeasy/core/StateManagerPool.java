package io.stateeasy.core;

import io.stateeasy.concurrent.EventExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class StateManagerPool {
    public static final StateManagerPool INSTANCE = new StateManagerPool();
    private static final Map<String, StateManager<?, ?>> stateManagers = new ConcurrentHashMap<>();

    private StateManagerPool() {
    }

    @SuppressWarnings("unchecked")
    public <STATE, EVENT> StateManager<STATE, EVENT> create(StateDef<STATE, EVENT> stateDef,
                                                            EventExecutor executor) {
        return (StateManager<STATE, EVENT>) stateManagers.compute(stateDef.name(),
                (k, v) -> {
                    if (v != null) {
                        throw new IllegalArgumentException("state manager " + k + ", already " +
                                "created");
                    }
                    return new SnapshotStateManager<>(executor, stateDef);
                });
    }

    void remove(StateManager<?, ?> stateManager) {
        stateManagers.remove(stateManager.name(), stateManager);
    }
}
