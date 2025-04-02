# StateEasy Persistent

响应式状态管理和持久化工具

## 介绍

StateEasy Persistent 是一个异步响应式的状态管理和持久化工具，旨在简化应用程序中的状态管理。通过自定义
`StateDef`和`EventSourceStateDef` 接口，用户能够灵活地定义状态更新逻辑以及持久化策略。本项目提供了一套完整的机制
来处理 状态的初始化、更新、查询以及持久化，并且支持事件驱动的设计模式。

## 功能特点

- **异步响应式设计**：所有操作都基于异步模型实现，确保了高并发场景下的性能。
- **可定制的状态与事件处理**：通过实现 `StateDef` 和 `EventSourceStateDef`
  接口，开发者可以完全控制状态如何根据特定事件进行更新。
- **自动化的状态持久化**：支持定期将内存中的状态数据持久化到存储中，并能在启动时恢复这些数据。
- **事件源集成**：对于需要跟踪和回放事件的应用程序，提供了专门的接口以方便地管理和存储事件流。

## 快速开始

### 引入依赖

maven：

```xml

<dependency>
    <groupId>io.stateeasy</groupId>
    <artifactId>stateeasy-persistent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

下面是如何使用 StateEasy 创建一个简单的计数器应用的例子。这个例子展示了如何定义状态类 (
`CounterState`) 、事件类 (`IncrementEvent`) 以及它们对应的序列化器，并通过 `StateManager` 来管理状态。

### 定义状态与事件

```java
// 定义计数器状态
public class CounterState {
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

// 定义时间，用来表示添加操作
public record IncrementEvent(String key, long num) {
}
```

### 定义序列化器

```java
// 事件序列化器
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

// 状态序列化器
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
```

### 实现状态定义

```java
public static class ExampleStateDef implements EventSourceStateDef<ExampleState, ExampleEvent> {
    // 定义数据存储路径
    private static final String dataStorePath = ".tmp/store";

    // 构造函数，初始化时创建数据存储目录
    public CounterStateDef() {
        new File(dataStorePath).mkdirs();
    }

    // 提供事件存储实现，使用本地文件系统作为存储介质，并指定序列化器
    @Override
    public EventStore<IncrementEvent> eventStore(EventExecutor executor) {
        LogFileEventStoreConfig config = new LogFileEventStoreConfig();
        config.setLogDir(dataStorePath);  // 设置日志存储目录
        return new LocalFileEventStore<>(config, new EventSerializer());  // 创建并返回事件存储实例
    }

    // 返回状态定义的名称
    @Override
    public String name() {
        return "example";
    }

    // 配置快照策略，默认配置
    @Override
    public SnapshotConfig snapshotConfig() {
        return new SnapshotConfig();
    }

    // 初始化状态，这里返回一个空的计数器状态
    @Override
    public CounterState initialState() {
        return new CounterState();
    }

    // 定义状态更新逻辑
    @Override
    public CounterState update(CounterState exampleState, IncrementEvent msg) {
        // 对指定键执行加法操作，并返回更新后的状态
        exampleState.incrementAndGet(msg.key(), msg.num());
        return exampleState;
    }

    // 提供状态存储实现，同样使用本地文件系统作为存储介质，并指定序列化器
    @Override
    public StateStore<CounterState> stateStore(EventExecutor executor) {
        LogFileEventStoreConfig config = new LogFileEventStoreConfig();
        config.setLogDir(dataStorePath);  // 设置日志存储目录
        return new LocalFileStateStore<>(config, new StateSerializer());  // 创建并返回状态存储实例
    }
}
```

### 使用 `StateManager` 管理状态

```java
public static void main(String[] args) throws Exception {
    EventExecutor executor = new DefaultSingleThreadEventExecutor();
    StateManager<CounterState, IncrementEvent> stateManager = StateManager.create(new CounterStateDef(), executor);
    stateManager.start().toFuture().get();
    Try<Long> get = stateManager.query(state -> state.get("key"))
            .toFuture()
            .syncUninterruptibly();
    // 输出初始计数器状态，若初次执行，结果应当是0，若重复执行多次，结果应当是上次执行时的最终结果
    System.out.println(get);
    stateManager.send(new IncrementEvent("key", 1));
    Try<Long> result = stateManager.sendAndQuery(new IncrementEvent("key", 2), state -> state.get("key"))
            .toFuture()
            .syncUninterruptibly();
    // 输出计数器更新后的结果
    System.out.println(result);

    stateManager.shutdown().toFuture().get();
    executor.shutdownAsync().join();
    System.exit(0);
}  
```

以上就是利用 StateEasy 构建基本应用的一个示例。更多详细信息，请参考代码库中的注释及文档。
