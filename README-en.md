# index-logging

选择语言: [中文](README.md)

## Introduction

This is a lightweight, high-performance, purely asynchronous message logging tool based on mmap. It
supports appending messages and reading messages based on indexes. It can be applied to various
scenarios such as Write-Ahead Logging (WAL), message queues, and event sourcing.

## Usage Example

### Custom Message Type

First, we need to define the data structure we want to log. Here, we use a simple Java record to
represent it:

```java
public record MyMessage(int id,
                        String message,
                        long timestamp) {
}
```

### Custom Serializer

To convert `MyMessage` objects into a byte stream for storage or transmission, we need to implement
a `Serializer` interface. Here is a simple implementation:

```java
import io.masterkun.commons.indexlogging.Serializer;

import java.io.IOException;

public class MySerializer implements Serializer<MyMessage> {
    @Override
    public void serialize(MyMessage obj, DataOut out) throws IOException {
        out.writeInt(obj.id());
        out.writeUTF(obj.message());
        out.writeLong(obj.timestamp());
    }

    @Override
    public MyMessage deserialize(DataIn in) throws IOException {
        return new MyMessage(in.readInt(), in.readUTF(), in.readLong());
    }
}
```

### Configure LogSystem and Get EventLogger

Next, we set up the `LogSystem` and request an `EventLogger` instance by specifying the
configuration and serializer, and then perform message read and write operations.

```java
import io.masterkun.commons.indexlogging.LogConfig;
import io.masterkun.commons.indexlogging.LogSystem;

import java.util.concurrent.atomic.AtomicInteger;

public class Example {
    public static void main(String[] args) throws Exception {
        // Initialize LogSystem
        LogSystem system = new LogSystem(4); // Assume each disk has a maximum of 4 writing threads
        // Log system configuration
        LogConfig logConfig = LogConfig
                .builder("example", new File(".tmp/example"))
                .readTimeout(Duration.ofMillis(1000))
                .build();
        // Get EventLogger
        EventLogger<MyMessage> eventLogger = system.get(logConfig, new MySerializer());

        // Write 100 messages
        for (int i = 0; i < 100; i++) {
            MyMessage msg = new MyMessage(i, "Hello", System.currentTimeMillis());
            eventLogger.write(msg).whenComplete((idAndOffset, error) -> {
                if (error != null) {
                    error.printStackTrace();
                } else {
                    System.out.println("Message written at: " + idAndOffset);
                }
            });
        }

        // Read messages in batches of 10, break the loop when no more messages are read
        AtomicInteger msgCount = new AtomicInteger();
        IdAndOffset idAndOffset = null;
        while (true) {
            CompletableFuture<IdAndOffset> future = new CompletableFuture<>();
            LogObserver<MyMessage> observer = new LogObserver<>() {
                @Override
                public void onNext(long id, long offset, MyMessage value) {
                    System.out.println("Received id=" + id + ", offset=" + offset + ", msg=" + value);
                    msgCount.incrementAndGet();
                }

                @Override
                public void onComplete(long nextId, long nextOffset) {
                    future.complete(new IdAndOffset(nextId, nextOffset));
                }

                @Override
                public void onError(Throwable e) {
                    future.completeExceptionally(e);
                }
            };
            if (idAndOffset == null) {
                eventLogger.read(0, 10, observer);
            } else {
                eventLogger.read(idAndOffset, 10, observer);
            }
            IdAndOffset nextIdAndOffset = future.join();
            if (Objects.equals(nextIdAndOffset, idAndOffset)) {
                System.out.println("Message read complete");
                break;
            }
            idAndOffset = nextIdAndOffset;
        }
        System.out.println("Total read: " + msgCount);
        system.shutdown().whenComplete((v, e) -> {
            if (e != null) {
                e.printStackTrace();
            }
            System.out.println("Shutdown");
        }).join();
    }
}
```

## Configuration Parameters

All log configuration parameters are in the `io.masterkun.commons.indexlogging.LogConfig` class:

| Name                  | Type     | Default Value | Description                                                     |
|-----------------------|----------|---------------|-----------------------------------------------------------------|
| name                  | String   |               | Log name                                                        |
| logDir                | String   |               | Directory to store log files                                    |
| segmentNum            | int      | 8             | Total number of log segment files to retain                     |
| segmentSizeMax        | int      | 16MB          | Maximum size of each log segment file (in bytes)                |
| indexChunkSize        | int      | 8KB           | Index chunk size (in bytes)                                     |
| indexPersistSize      | int      | 64KB          | Threshold size to trigger index persistence (in bytes)          |
| indexPersistInterval  | Duration | 10ms          | Interval to trigger index persistence                           |
| autoFlushSize         | int      | 8KB           | Threshold size to trigger automatic data persistence (in bytes) |
| autoFlushInterval     | Duration | 10ms          | Interval to trigger automatic data persistence                  |
| serializeBufferDirect | boolean  | true          | Whether to use direct buffer for serialization                  |
| serializeBufferInit   | int      | 4KB           | Initial size of serialization buffer (in bytes)                 |
| serializeBufferMax    | int      | 1MB           | Maximum size of serialization buffer (in bytes)                 |
| readTimeout           | Duration | 10s           | Read timeout                                                    |

## Benchmark

// TODO

## License

This project is licensed under the MIT License. For more information, see the [LICENSE](LICENSE)
file.

## Contact

If you have any questions or suggestions, please contact [jsczcmk@outlook.com].
