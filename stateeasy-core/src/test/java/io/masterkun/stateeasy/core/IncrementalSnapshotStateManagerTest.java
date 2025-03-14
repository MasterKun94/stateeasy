package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.core.SnapshotStateManagerTest.TestEvent;
import io.masterkun.stateeasy.core.StateManagerTestKit.TestState;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;

public class IncrementalSnapshotStateManagerTest {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();

    @Test
    public void testIncrementalSnapshotState() throws Exception {
        StateStoreAdaptor<TestState> stateStore = new StateStoreAdaptor<>(new TestStateStore<>());
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        manager.send(new TestEvent("key", "value")).toFuture().get();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());
        Snapshot<TestState> read = stateStore.read(executor.newPromise()).toFuture().get();
        Assert.assertNull(read);
        Thread.sleep(120);
        read = stateStore.read(executor.newPromise()).toFuture().get();
        Assert.assertNotNull(read);
        Assert.assertEquals(0L, read.snapshotId());
        Assert.assertEquals("value", read.state().get("key"));

        manager.shutdown().toFuture().get();

        manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());
    }


    @Test
    public void testIncrementalEventSourceState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        EventStore<TestEvent> eventStore = new TestEventStore<>();
        var stateDef = new TestEventSourceStateDef(stateStore, eventStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        manager.send(new TestEvent("key", "value")).toFuture().get();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());

        manager.send(new TestEvent("key", "value2")).toFuture().get();
        Assert.assertEquals("value2", manager.query(state -> state.get("key")).toFuture().get());
        manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        Assert.assertEquals("value2", manager.query(state -> state.get("key")).toFuture().get());
        manager.shutdown();

    }


    @Test
    public void testSendAndQuery() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        String result = manager.sendAndQuery(new TestEvent("key", "value"), state -> state.get(
                "key")).toFuture().get();
        Assert.assertEquals("value", result);
        manager.shutdown().toFuture().get();
    }

    @Test
    public void testSendAndQueryWithLazyMerge() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);
        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        String result = manager.sendAndQuery(new TestEvent("key", "value"), state -> state.get(
                "key")).toFuture().get();
        Assert.assertEquals("value", result);
        manager.shutdown().toFuture().get();
    }

    @Test
    public void testQueryWithSimpleState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        Assert.assertEquals(0, manager.query(TestState::size).toFuture().get().intValue());
        manager.shutdown().toFuture().get();
    }

    @Test
    public void testQueryWithUpdatedState() throws Exception {
        StateStore<TestState> stateStore = new TestStateStore<>();
        var stateDef = new TestStateDef(stateStore);

        StateManager<TestState, TestEvent> manager = new IncrementalSnapshotStateManager<>(
                executor, stateDef);
        manager.start().toFuture().get();
        manager.send(new TestEvent("key", "value")).toFuture().get();
        Assert.assertEquals(1, manager.query(TestState::size).toFuture().get().intValue());
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());
        manager.shutdown().toFuture().get();
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
        public StateStore<TestState> stateStore(EventExecutor executor) {
            return stateStore;
        }
    }

    public static class TestEventSourceStateDef extends TestStateDef implements EventSourceStateDef<TestState, TestEvent> {

        private final EventStore<TestEvent> eventStore;

        public TestEventSourceStateDef(StateStore<TestState> stateStore,
                                       EventStore<TestEvent> eventStore) {
            super(stateStore);
            this.eventStore = eventStore;
        }

        @Override
        public EventStore<TestEvent> eventStore(ScheduledExecutorService executor) {
            return eventStore;
        }
    }
}
