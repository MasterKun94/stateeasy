# index-logging

Choose Language: [English](README-en.md)

## 介绍

这是一个基于mmap的纯异步、轻量级、高性能的消息日志读写工具，支持消息的追加写入和基于索引的消息读取。
可应用于WAL、消息队列、事件溯源等多种场景。

## 使用样例

### 自定义消息类型

首先，我们需要定义想要记录的数据结构。这里采用一个简单的 Java 类来表示：

```java
public record MyMessage(int id,
                        String message,
                        long timestamp) {
}
```

### 自定义序列化器

为了将 `MyMessage` 对象转换为字节流以便存储或传输，我们需要实现一个 `Serializer` 接口。这里提供了一个简单实现：

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

### 配置 LogSystem 并获取 EventLogger

接下来是设置 `LogSystem` 并通过指定配置与序列化器来请求一个 `EventLogger` 实例，并进行消息读写。

```java

import io.masterkun.commons.indexlogging.LogConfig;
import io.masterkun.commons.indexlogging.LogSystem;

import java.util.concurrent.atomic.AtomicInteger;

public class Example {


    public static void main(String[] args) throws Exception {
        // 初始化 LogSystem
        LogSystem system = new LogSystem(4); // 假设每个磁盘有最多4个写入线程
        // 日志系统配置
        LogConfig logConfig = LogConfig
                .builder("example", new File(".tmp/example"))
                .readTimeout(Duration.ofMillis(1000))
                .build();
        // 获取 EventLogger
        EventLogger<MyMessage> eventLogger = system.get(logConfig, new MySerializer());

        // 写入100条消息
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

        // 每10条一批读取消息，当读到的消息条数为0时跳出循环
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
            IdAndOffset nextIdAndOffset = future.get();
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

## 参数配置

日志参数配置都在`io.masterkun.commons.indexlogging.LogConfig`类中:

| 名称                    | 类型       | 默认值  | 说明                    |
|-----------------------|----------|------|-----------------------|
| name                  | String   |      | 日志名称                  |
| logDir                | String   |      | 存储日志文件的目录             |
| segmentNum            | int      | 8    | 保留的日志段文件总数量           |
| segmentSizeMax        | int      | 16MB | 每个日志段文件的最大大小（单位字节）    |
| indexChunkSize        | int      | 8KB  | 索引区块大小（单位字节）          |
| indexPersistSize      | int      | 64KB | 触发索引持久化的阈值大小（单位字节）    |
| indexPersistInterval  | Duration | 10ms | 触发索引持久化的时间间隔          |
| autoFlushSize         | int      | 8KB  | 写数据自动持久化的阈值大小（以字节为单位） |
| autoFlushInterval     | Duration | 10ms | 写数据自动持久化的时间间隔         |
| serializeBufferDirect | boolean  | true | 是否使用直接缓冲区进行序列化        |
| serializeBufferInit   | int      | 4KB  | 序列化缓冲区的初始大小（以字节为单位）   |
| serializeBufferMax    | int      | 1MB  | 序列化缓冲区的最大大小（以字节为单位）   |
| readTimeout           | Duration | 10s  | 读取超时时间                |

## Benchmark

// TODO

## 许可

本项目采用 MIT 许可证。更多信息请参见 [LICENSE](LICENSE) 文件。

## 联系

如果有任何问题或建议，请联系 [jsczcmk@outlook.com]。
