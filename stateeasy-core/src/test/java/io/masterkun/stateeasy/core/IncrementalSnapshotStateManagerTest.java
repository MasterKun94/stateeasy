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
        var stateDef = new TestStateDef(stateStore);

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


    @Test
    public void testIncrementalEventSourceState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        EventStore<TestEvent> eventStore = new TestEventStore<>();
        var stateDef = new TestEventSourceStateDef(stateStore, eventStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        manager.send(new TestEvent("key", "value")).join();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).join());

        manager.send(new TestEvent("key", "value2")).join();
        Assert.assertEquals("value2", manager.query(state -> state.get("key")).join());
        manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        Assert.assertEquals("value2", manager.query(state -> state.get("key")).join());
        manager.shutdown();

    }


    @Test
    public void testSendAndQuery() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        String result = manager.sendAndQuery(new TestEvent("key", "value"), state -> state.get("key")).join();
        Assert.assertEquals("value", result);
        manager.shutdown().join();
    }

    @Test
    public void testSendAndQueryWithLazyMerge() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);
        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        String result = manager.sendAndQuery(new TestEvent("key", "value"), state -> state.get("key")).join();
        Assert.assertEquals("value", result);
        manager.shutdown().join();
    }

    @Test
    public void testQueryWithSimpleState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        Assert.assertEquals(0, manager.query(TestState::size).join().intValue());
        manager.shutdown().join();
    }

    @Test
    public void testQueryWithUpdatedState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                Executors.newSingleThreadScheduledExecutor(), stateDef);
        manager.start().join();
        manager.send(new TestEvent("key", "value")).join();
        Assert.assertEquals(1, manager.query(TestState::size).join().intValue());
        Assert.assertEquals("value", manager.query(state -> state.get("key")).join());
        manager.shutdown().join();
    }

    public static class TestStateDef implements IncrementalSnapshotStateDef<TestState, TestEvent> {
        private final StateStore<TestState> stateStore;

        public TestStateDef(StateStore<TestState> stateStore) {
            this.stateStore = stateStore;
        }

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
    }

    public static class TestEventSourceStateDef extends TestStateDef implements EventSourceStateDef<TestState, TestEvent> {

        private final EventStore<TestEvent> eventStore;

        public TestEventSourceStateDef(StateStore<TestState> stateStore, EventStore<TestEvent> eventStore) {
            super(stateStore);
            this.eventStore = eventStore;
        }

        @Override
        public EventStore<TestEvent> eventStore(ScheduledExecutorService executor) {
            return eventStore;
        }
    }
}
