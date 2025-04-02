package io.stateeasy.core.impl;

import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.core.Snapshot;
import io.stateeasy.core.SnapshotAndId;
import io.stateeasy.core.StateDef;
import io.stateeasy.core.StateStoreAdaptor;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class MemoryStateStoreTest {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();

    @Test
    public void testWriteWithNonIncrementalState() throws Exception {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        StateStoreAdaptor<String> store = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        store.initialize(stateDef, executor.newPromise());

        Snapshot<String> snapshot1 = new Snapshot<>("state1", 10, Map.of());
        Long l = store.write(snapshot1, executor.newPromise()).toFuture().get();
        assertEquals(0, l.longValue());

        var result = store.read(executor.newPromise());
        assertEquals(new SnapshotAndId<>(l, snapshot1), result.toFuture().get());
    }

    @Test
    public void testWriteWithMultipleSnapshots() throws Exception {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        StateStoreAdaptor<String> store = new StateStoreAdaptor<>(new MemoryStateStore<>(executor));
        store.initialize(stateDef, executor.newPromise());

        Snapshot<String> snapshot1 = new Snapshot<>("state1", 10, Map.of());
        Long l = store.write(snapshot1, executor.newPromise()).toFuture().get();
        assertEquals(0, l.longValue());

        Snapshot<String> snapshot2 = new Snapshot<>("state2", 20, Map.of());
        l = store.write(snapshot2, executor.newPromise()).toFuture().get();
        assertEquals(1, l.longValue());

        var result = store.read(executor.newPromise());
        assertEquals(new SnapshotAndId<>(l, snapshot2), result.toFuture().get());
    }
}
