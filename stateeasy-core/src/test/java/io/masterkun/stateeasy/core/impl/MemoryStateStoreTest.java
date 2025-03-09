package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.IncrementalSnapshotStateDef;
import io.masterkun.stateeasy.core.Snapshot;
import io.masterkun.stateeasy.core.StateDef;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class MemoryStateStoreTest {

    @Test
    public void testWriteWithNonIncrementalState() {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        MemoryStateStore<String> store = new MemoryStateStore<>();
        store.initialize(stateDef);

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1);

        var result = store.read();
        assertEquals(snapshot1, result.join());
    }

    @Test
    public void testWriteWithIncrementalState() {
        IncrementalSnapshotStateDef<String, ?> stateDef = mock(IncrementalSnapshotStateDef.class);
        when(stateDef.merge(any(), any())).thenAnswer(invocation -> {
            String state1 = invocation.getArgument(0);
            String state2 = invocation.getArgument(1);
            return state1 + state2;
        });

        MemoryStateStore<String> store = new MemoryStateStore<>();
        store.initialize(stateDef);

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1);

        Snapshot<String> snapshot2 = new Snapshot<>(2, "state2", 20, Map.of());
        store.write(snapshot2);

        var result = store.read();
        assertEquals(new Snapshot<>(2, "state1state2", 20, Map.of()), result.join());
    }

    @Test
    public void testWriteWithMultipleSnapshots() {
        StateDef<String, ?> stateDef = mock(StateDef.class);
        MemoryStateStore<String> store = new MemoryStateStore<>();
        store.initialize(stateDef);

        Snapshot<String> snapshot1 = new Snapshot<>(1, "state1", 10, Map.of());
        store.write(snapshot1);

        Snapshot<String> snapshot2 = new Snapshot<>(2, "state2", 20, Map.of());
        store.write(snapshot2);

        var result = store.read();
        assertEquals(snapshot2, result.join());
    }
}
