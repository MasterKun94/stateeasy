package io.stateeasy.concurrent.example;

import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.EventPromise;
import io.stateeasy.concurrent.EventStage;
import io.stateeasy.concurrent.Try;

public class EventPromiseExample {
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
}
