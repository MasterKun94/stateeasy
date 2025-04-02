# StateEasy Concurrent

异步任务调度工具类库

## 简介

本工具类库提供了响应式编程和异步任务调度的能力。它包含了一系列的接口与类，如`EventStage`、
`EventExecutor`
以及`EventPromise`等，旨在简化并发操作的管理，并提供了一种处理异步事件的方法。该工具包支持异步执行任务、添加超时处理、监听事件完成状态等功能。

## 特性

- **响应式编程模型**：基于`EventStage`接口构建了灵活且强大的异步事件处理机制。
- **异步任务执行**：通过`EventExecutor`实现任务的异步执行，并可配置单线程或其它类型的执行器来满足不同场景需求。
- **超时控制**：允许为异步任务设置超时时间，在指定时间内未完成的任务将被自动取消并抛出异常。
- **事件监听**：用户可以通过注册`EventStageListener`来监听异步任务的成功或失败情况。
- **易于使用**：提供了简洁明了的API设计，方便开发者快速上手并集成到项目中。

## 快速开始

要开始使用此工具类库，请首先确保你的项目依赖中包含了相关库。然后，你可以参照以下示例代码来了解如何创建一个简单的异步任务链：

### 引入依赖

maven：

```xml

<dependency>
    <groupId>io.stateeasy</groupId>
    <artifactId>stateeasy-concurrent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### 示例 - 使用 `EventStage`

下面的例子展示了如何利用`EventStage`来组合多个异步操作，并在最终结果到达时打印输出信息。

```java
import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.EventStage;
import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.concurrent.Try;

public class EventStageExample {
    private static final EventExecutor executor1 = new DefaultSingleThreadEventExecutor();
    private static final EventExecutor executor2 = new DefaultSingleThreadEventExecutor();

    public static void main(String[] args) {
        // 使用supplyAsync方法创建一个异步任务，并指定使用executor1来执行runSyncTask1方法
        Try<String> result = EventStage.supplyAsync(EventStageExample::runSyncTask1, executor1)
                // 对上一步的结果应用map操作，将结果传递给runSyncTask2方法进行处理
                .map(EventStageExample::runSyncTask2)
                // 将上一步的结果传递给runAsyncTask方法，并返回一个新的EventStage，该方法内部也是异步执行的
                .flatMap(EventStageExample::runAsyncTask)
                // 为整个异步任务链添加监听器，当任务成功或失败时分别调用success和failure方法
                .addListener(new EventStageListener<>() {
                    @Override
                    public void success(String value) {
                        // 如果任务成功完成，则打印最终结果
                        System.out.println("Output: " + value);
                    }

                    @Override
                    public void failure(Throwable cause) {
                        // 如果任务执行过程中出现异常，则打印堆栈信息
                        cause.printStackTrace();
                    }
                })
                // 将EventStage转换为Future并同步等待其完成
                .toFuture().syncUninterruptibly();
        // 打印Try对象，其中包含了任务的结果或异常信息
        System.out.println(result);

        // 关闭两个事件执行器，释放资源
        executor1.shutdown();
        executor2.shutdown();
    }

    private static String runSyncTask1() {
        return "Hello";
    }

    private static String runSyncTask2(String input) {
        return input + " World";
    }

    private static EventStage<String> runAsyncTask(String input) {
        return EventStage.supplyAsync(() -> input + "!", executor2);
    }
}
```

### 示例 - 使用 `EventPromise`

```java
// 创建一个主线程执行器，用于处理主逻辑任务
private static final EventExecutor mainExecutor = new DefaultSingleThreadEventExecutor();
// 创建一个任务执行器，用于实际执行异步任务
private static final EventExecutor taskExecutor = new DefaultSingleThreadEventExecutor();

public static void main(String[] args) {
    // 使用EventPromise创建一个新的异步任务，并指定使用mainExecutor来执行
    Try<String> result = runAsync(EventPromise.newPromise(mainExecutor))
            // 对上一步的结果应用map操作，将结果与" World"拼接
            .map(str -> str + " World")
            // 将EventStage转换为Future并同步等待其完成
            .toFuture()
            .syncUninterruptibly();
    // 打印Try对象，其中包含了任务的结果或异常信息
    System.out.println(result);
    // 关闭两个事件执行器，释放资源
    mainExecutor.shutdown();
    taskExecutor.shutdown();
}

// 定义一个异步任务方法，接收一个EventPromise作为参数
private static EventStage<String> runAsync(EventPromise<String> promise) {
    // 在taskExecutor中执行异步任务
    taskExecutor.execute(() -> {
        try {
            // 模拟任务执行过程，这里使用Thread.sleep(100)来模拟耗时操作
            Thread.sleep(100);
            // 任务成功完成，通过promise.success方法传递结果
            promise.success("Hello");
        } catch (Throwable e) {
            // 如果任务执行过程中出现异常，则通过promise.failure方法传递异常
            promise.failure(e);
        }
    });
    // 返回包含异步任务的EventPromise
    return promise;
}

private static final EventExecutor mainExecutor = new DefaultSingleThreadEventExecutor();
private static final EventExecutor taskExecutor = new DefaultSingleThreadEventExecutor();

public static void main(String[] args) {
    Try<String> result = runAsync(EventPromise.newPromise(mainExecutor))
            .map(str -> str + " World")
            .toFuture()
            .syncUninterruptibly();
    System.out.println(result);
    mainExecutor.shutdown();
    taskExecutor.shutdown();
}

private static EventStage<String> runAsync(EventPromise<String> promise) {
    taskExecutor.execute(() -> {
        try {
            // running task
            Thread.sleep(100);
            promise.success("Hello");
        } catch (Throwable e) {
            promise.failure(e);
        }
    });
    return promise;
}
```
