package io.stateeasy.core;

import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.core.StateManagerTestKit.TestState;
import org.junit.Assert;
import org.junit.Test;

import java.time.Duration;

public class SnapshotStateManagerTest {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();

    @Test
    public void testSnapshotState() throws Exception {
        StateStoreAdaptor<TestState> stateStore = new StateStoreAdaptor<>(new TestStateStore<>());
        var stateDef = new StateDef<TestState, TestEvent>() {
            @Override
            public String name() {
                return "test";
            }

            @Override
            public SnapshotConfig snapshotConfig() {
                SnapshotConfig config = new SnapshotConfig();
                config.setSnapshotInterval(Duration.ofMillis(100));
                config.setAutoExpire(true);
                return config;
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
        };
        StateManager<TestState, TestEvent> manager = new SnapshotStateManager<>(executor, stateDef);
        manager.start().toFuture().get();
        manager.send(new TestEvent("key", "value")).toFuture().get();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());
        SnapshotAndId<TestState> read = stateStore.read(executor.newPromise()).toFuture().get();
        Assert.assertNull(read);
        Thread.sleep(120);
        read = stateStore.read(executor.newPromise()).toFuture().get();
        Assert.assertNotNull(read);
        Assert.assertEquals(0L, read.snapshotId());
        Assert.assertEquals("value", read.state().get("key"));

        manager.shutdown().toFuture().get();

        manager = new SnapshotStateManager<>(executor, stateDef);
        manager.start().toFuture().get();
        Assert.assertEquals("value", manager.query(state -> state.get("key")).toFuture().get());
    }

    public record TestEvent(String key, String value) {

    }

}
