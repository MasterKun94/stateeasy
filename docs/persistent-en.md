# StateEasy Persistent

Reactive State Management and Persistence Tool

## Introduction

StateEasy Persistent is an asynchronous, reactive state management and persistence tool designed to
simplify state handling in applications. By customizing the `StateDef` and `EventSourceStateDef`
interfaces, users can flexibly define their own logic for state updates and persistence strategies.
This project offers a comprehensive mechanism for initializing, updating, querying, and persisting
states, supporting an event-driven design pattern.

## Features

- **Asynchronous and Reactive Design**: All operations are implemented based on an asynchronous
  model, ensuring performance under high concurrency scenarios.
- **Customizable State and Event Handling**: Developers have full control over how states should be
  updated according to specific events by implementing the `StateDef` and `EventSourceStateDef`
  interfaces.
- **Automated State Persistence**: Supports periodic persistence of in-memory state data to storage
  and can restore this data upon startup.
- **Event Source Integration**: For applications requiring event tracking and replay, dedicated
  interfaces are provided for managing and storing event streams conveniently.

## Getting Started

### Adding Dependencies

For Maven, add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.masterkun.commons</groupId>
    <artifactId>stateeasy-persistent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Below is an example of how to use StateEasy to create a simple counter application. This example
illustrates how to define a state class (`CounterState`), an event class (`IncrementEvent`), along
with their serializers, and manage the state using `StateManager`.

### Defining States and Events

```java
// Define the counter state
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

// Define the event to represent an increment operation
public record IncrementEvent(String key, long num) {
}
```

### Defining Serializers

```java
// Event serializer
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

// State serializer
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

### Implementing State Definitions

```java
public static class ExampleStateDef implements EventSourceStateDef<ExampleState, ExampleEvent> {
    // Define the data store path
    private static final String dataStorePath = ".tmp/store";

    // Constructor, creates the data storage directory during initialization
    public CounterStateDef() {
        new File(dataStorePath).mkdirs();
    }

    // Provides the event store implementation, using the local file system as the storage medium and specifying the serializer
    @Override
    public EventStore<IncrementEvent> eventStore(EventExecutor executor) {
        LogFileEventStoreConfig config = new LogFileEventStoreConfig();
        config.setLogDir(dataStorePath);  // Set the log storage directory
        return new LocalFileEventStore<>(config, new EventSerializer());  // Create and return the event store instance
    }

    // Returns the name of the state definition
    @Override
    public String name() {
        return "example";
    }

    // Configures the snapshot strategy, default configuration
    @Override
    public SnapshotConfig snapshotConfig() {
        return new SnapshotConfig();
    }

    // Initializes the state, here returning an empty counter state
    @Override
    public CounterState initialState() {
        return new CounterState();
    }

    // Defines the state update logic
    @Override
    public CounterState update(CounterState exampleState, IncrementEvent msg) {
        // Perform addition on the specified key and return the updated state
        exampleState.incrementAndGet(msg.key(), msg.num());
        return exampleState;
    }

    // Provides the state store implementation, also using the local file system as the storage medium and specifying the serializer
    @Override
    public StateStore<CounterState> stateStore(EventExecutor executor) {
        LogFileEventStoreConfig config = new LogFileEventStoreConfig();
        config.setLogDir(dataStorePath);  // Set the log storage directory
        return new LocalFileStateStore<>(config, new StateSerializer());  // Create and return the state store instance
    }
}
```

### Managing States with `StateManager`

```java
public static void main(String[] args) throws Exception {
    EventExecutor executor = new DefaultSingleThreadEventExecutor();
    StateManager<CounterState, IncrementEvent> stateManager = new StateManager<>(new ExampleStateDef(), executor);
    // Further code to demonstrate usage...
}
```

This guide provides a basic introduction to getting started with StateEasy. For more detailed
information, including advanced features and best practices, please refer to
the [full documentation](#).
