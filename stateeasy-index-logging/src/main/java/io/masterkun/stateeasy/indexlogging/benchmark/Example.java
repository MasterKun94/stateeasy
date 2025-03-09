package io.masterkun.stateeasy.indexlogging.benchmark;

import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogConfig;
import io.masterkun.stateeasy.indexlogging.LogObserver;
import io.masterkun.stateeasy.indexlogging.LogSystem;
import io.masterkun.stateeasy.indexlogging.Serializer;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class Example {

    static {
        File f = new File(".tmp/example");
        if (f.isDirectory()) {
            for (File file : Objects.requireNonNull(f.listFiles())) {
                file.delete();
            }
        } else {
            f.mkdirs();
        }
    }

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

class MySerializer implements Serializer<MyMessage> {
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

record MyMessage(int id,
                 String message,
                 long timestamp) {
}
