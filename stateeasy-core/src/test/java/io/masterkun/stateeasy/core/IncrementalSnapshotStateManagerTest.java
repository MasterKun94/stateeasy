package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.core.SnapshotStateManagerTest.TestEvent;
import io.masterkun.stateeasy.core.StateManagerTestKit.TestState;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class IncrementalSnapshotStateManagerTest {

    @Test
    public void testIncrementalSnapshotState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new IncrementalSnapshotStateDef<TestState, TestEvent>() {
            @Override
            public IncrementalSnapshotConfig incrementalSnapshotConfig() {
                return new IncrementalSnapshotConfig(true);
            }

            @Override
            public TestState merge(TestState previous, TestState incremental) {
                return previous.merge(incremental);
            }

            @Override
            public SnapshotConfig snapshotConfig() {
                return new SnapshotConfig(Duration.ofMillis(100));
            }

            @Override
            public TestState initialState() {
                return new TestState();
            }

            @Override
            public TestState update(TestState testState, TestEvent msg) {
                testState.put(msg);
                return testState;
            }

            @Override
            public StateStore<TestState> stateStore(ScheduledExecutorService executor) {
                return stateStore;
            }
        };
        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        manager.send(new TestEvent("key", "value")).join();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).join());
        Snapshot<TestState> read = stateStore.read().join();
        Assert.assertNull(read);
        Thread.sleep(120);
        read = stateStore.read().join();
        Assert.assertNotNull(read);
        Assert.assertEquals(0L, read.snapshotId());
        Assert.assertEquals("value", read.state().get("key"));

        manager.shutdown().join();

        manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).join());
    }
}
