# StateEasy Concurrent

## Introduction

StateEasy-Concurrent is a library designed for reactive programming and asynchronous task
scheduling. It provides a set of interfaces and classes, such as `EventStage`, `EventExecutor`, and
`EventPromise`, to simplify the management of concurrent operations and provide a robust mechanism
for handling asynchronous events. The library supports asynchronous task execution, timeout
handling, and event completion state listening.

## Features

- **Reactive Programming Model**: Built on the `EventStage` interface, it offers a flexible and
  powerful mechanism for handling asynchronous events.
- **Asynchronous Task Execution**: Tasks can be executed asynchronously using `EventExecutor`, which
  can be configured with single-threaded or other types of executors to meet different requirements.
- **Timeout Control**: Allows setting timeouts for asynchronous tasks, automatically canceling and
  throwing exceptions for tasks that do not complete within the specified time.
- **Event Listening**: Users can register `EventStageListener` to listen for the success or failure
  of asynchronous tasks.
- **Ease of Use**: Provides a clear and concise API design, making it easy for developers to quickly
  get started and integrate into their projects.

## Getting Started

To start using this library, first ensure that your project includes the necessary dependencies.
Then, you can refer to the example code below to understand how to create a simple asynchronous task
chain.

### Adding Dependencies

For Maven, add the following dependency to your `pom.xml`:

```xml

<dependency>
    <groupId>io.stateeasy</groupId>
    <artifactId>stateeasy-concurrent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

### Example - Using `EventStage`

The following example demonstrates how to use `EventStage` to chain multiple asynchronous operations
and print the final result when it is available.

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
        Try<String> result = EventStage.supplyAsync(() -> "Hello", executor1)
                .map(str -> str + " World")
                .flatMap(str -> EventStage.supplyAsync(() -> str + "!", executor2))
                .addListener(new EventStageListener<>() {
                    @Override
                    public void success(String value) {
                        System.out.println("Output: " + value);
                    }

                    @Override
                    public void failure(Throwable cause) {
                        cause.printStackTrace();
                    }
                })
                .toFuture().syncUninterruptibly();
        System.out.println(result);
        executor1.shutdown();
        executor2.shutdown();
    }
}
```

### Example - Using `EventPromise`

For more details on how to use `EventPromise`, please refer to the `EventPromiseExample` file.

## Additional Resources

- Refer to the full documentation for more information about each component.
- Explore the [example directory](./example/) for more use cases.
- If you encounter any issues, check the FAQ or contact our support team.

We hope this introduction helps you quickly grasp the basic concepts and usage of this library!
