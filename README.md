# index-logging

## 介绍

这是一个基于mmap的轻量高性能消息日志读写工具，支持消息的追加写入，和基于索引的消息读取。
可应用于WAL、消息队列、时间溯源等多种场景。

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
import io.masterkun.commons.indexlogging.Serializer;import java.io.IOException;

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

接下来是设置 `LogSystem` 并通过指定配置与序列化器来请求一个 `EventLogger` 实例。此步骤还包括了如何处理可能抛出的异常。
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
    }
}
```



## 许可

本项目采用 MIT 许可证。更多信息请参见 [LICENSE](LICENSE) 文件。

## 联系

如果有任何问题或建议，请联系 [jsczcmk@outlook.com]。
