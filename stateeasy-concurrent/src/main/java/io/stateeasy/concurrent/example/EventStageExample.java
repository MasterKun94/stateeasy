package io.stateeasy.concurrent.example;

import io.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.stateeasy.concurrent.EventExecutor;
import io.stateeasy.concurrent.EventStage;
import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.concurrent.Try;

public class EventStageExample {
    private static final EventExecutor executor1 = new DefaultSingleThreadEventExecutor();
    private static final EventExecutor executor2 = new DefaultSingleThreadEventExecutor();

    public static void main(String[] args) {
        Try<String> result = EventStage.supplyAsync(EventStageExample::runSyncTask1, executor1)
                .map(EventStageExample::runSyncTask2)
                .flatmap(EventStageExample::runAsyncTask)
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
