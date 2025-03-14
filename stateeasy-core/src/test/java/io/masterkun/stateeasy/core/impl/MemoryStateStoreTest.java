package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.core.IncrementalSnapshotStateDef;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.StateDef;
import io.masterkun.stateeasy.core.StateStoreAdaptor;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MemoryStateStoreTest {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();

    @Test
    public void testWriteWithNonIncrementalState() throws Exception {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        StateStoreAdaptor<String> store = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        store.initialize(stateDef, executor.newPromise());

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1, executor.newPromise());

        var result = store.read(executor.newPromise());
        assertEquals(snapshot1, result.toFuture().get());
    }

    @Test
    public void testWriteWithIncrementalState() throws Exception {
        IncrementalSnapshotStateDef<String, ?> stateDef = mock(IncrementalSnapshotStateDef.class);
        when(stateDef.merge(any(), any())).thenAnswer(invocation -> {
            String state1 = invocation.getArgument(0);
            String state2 = invocation.getArgument(1);
            return state1 + state2;
        });

        StateStoreAdaptor<String> store = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        store.initialize(stateDef, executor.newPromise());

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1, executor.newPromise());

        Snapshot<String> snapshot2 = new Snapshot<>(2, "state2", 20, Map.of());
        store.write(snapshot2, executor.newPromise());

        var result = store.read(executor.newPromise());
        assertEquals(new Snapshot<>(2, "state1state2", 20, Map.of()), result.toFuture().get());
    }

    @Test
    public void testWriteWithMultipleSnapshots() throws Exception {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        StateStoreAdaptor<String> store = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        store.initialize(stateDef, executor.newPromise());

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1, executor.newPromise());

        Snapshot<String> snapshot2 = new Snapshot<>(2, "state2", 20, Map.of());
        store.write(snapshot2, executor.newPromise());

        var result = store.read(executor.newPromise());
        assertEquals(snapshot2, result.toFuture().get());
    }
}
