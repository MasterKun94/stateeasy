package io.masterkun.stateeasy.core.example;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.Try;
import io.masterkun.stateeasy.core.EventSourceStateDef;
import io.masterkun.stateeasy.core.EventStore;
import io.masterkun.stateeasy.core.SnapshotConfig;
import io.masterkun.stateeasy.core.StateManager;
import io.masterkun.stateeasy.core.StateStore;
import io.masterkun.stateeasy.core.impl.LocalFileEventStore;
import io.masterkun.stateeasy.core.impl.LocalFileStateStore;
import io.masterkun.stateeasy.core.impl.LogFileEventStoreConfig;
import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Example {

    public static void main(String[] args) throws Exception {
        EventExecutor executor = new DefaultSingleThreadEventExecutor();
        StateManager<CounterState, IncrementEvent> stateManager =
                StateManager.create(new CounterStateDef(), executor);
        stateManager.start().toFuture().get();
        Try<Long> get = stateManager.query(state -> state.get("key"))
                .toFuture()
                .syncUninterruptibly();
        // 输出初始计数器状态，若初次执行，结果应当是0，若重复执行多次，结果应当是上次执行时的最终结果
        System.out.println(get);
        stateManager.send(new IncrementEvent("key", 1));
        Try<Long> result = stateManager.sendAndQuery(new IncrementEvent("key", 2),
                        state -> state.get("key"))
                .toFuture()
                .syncUninterruptibly();
        // 输出计数器更新后的结果
        System.out.println(result);

        stateManager.shutdown().toFuture().get();
        executor.shutdownAsync().join();
        System.exit(0);
    }

    public record IncrementEvent(String key, long num) {
    }

    ;

    public static class CounterState {
        private final Map<String, Long> map;

        public CounterState(Map<String, Long> map) {
            this.map = map;
        }

        public CounterState() {
            this(new HashMap<>());
        }

        public long get(String key) {
            return map.getOrDefault(key, 0L);
        }

        public long incrementAndGet(String key, long num) {
            return map.compute(key, (k, l) -> l == null ? num : num + l);
        }
    }

    public static class EventSerializer implements Serializer<IncrementEvent> {

        @Override
        public void serialize(IncrementEvent obj, DataOut out) throws IOException {
            out.writeUTF(obj.key());
            out.writeLong(obj.num());
        }

        @Override
        public IncrementEvent deserialize(DataIn in) throws IOException {
            return new IncrementEvent(in.readUTF(), in.readLong());
        }
    }

    public static class StateSerializer implements Serializer<CounterState> {

        @Override
        public void serialize(CounterState obj, DataOut out) throws IOException {
            out.writeInt(obj.map.size());
            for (Map.Entry<String, Long> entry : obj.map.entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeLong(entry.getValue());
            }
        }

        @Override
        public CounterState deserialize(DataIn in) throws IOException {
            int size = in.readInt();
            Map<String, Long> map = new HashMap<>(size);
            for (int i = 0; i < size; i++) {
                map.put(in.readUTF(), in.readLong());
            }
            return new CounterState(map);
        }
    }

    public static class CounterStateDef implements EventSourceStateDef<CounterState,
            IncrementEvent> {
        private static final String dataStorePath = ".tmp/store";

        public CounterStateDef() {
            new File(dataStorePath).mkdirs();
        }

        @Override
        public EventStore<IncrementEvent> eventStore(EventExecutor executor) {
            LogFileEventStoreConfig config = new LogFileEventStoreConfig();
            config.setLogDir(dataStorePath);
            return new LocalFileEventStore<>(config, new EventSerializer());
        }

        @Override
        public String name() {
            return "example";
        }

        @Override
        public SnapshotConfig snapshotConfig() {
            return new SnapshotConfig();
        }

        @Override
        public CounterState initialState() {
            return new CounterState();
        }

        @Override
        public CounterState update(CounterState exampleState, IncrementEvent msg) {
            exampleState.incrementAndGet(msg.key(), msg.num());
            return exampleState;
        }

        @Override
        public StateStore<CounterState> stateStore(EventExecutor executor) {
            LogFileEventStoreConfig config = new LogFileEventStoreConfig();
            config.setLogDir(dataStorePath);
            return new LocalFileStateStore<>(config, new StateSerializer());
        }
    }
}
