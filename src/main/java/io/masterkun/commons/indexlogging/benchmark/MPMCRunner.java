package io.masterkun.commons.indexlogging.benchmark;

import io.masterkun.commons.indexlogging.EventLogger;
import io.masterkun.commons.indexlogging.IdAndOffset;
import io.masterkun.commons.indexlogging.LogObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.BiConsumer;


public class MPMCRunner {
    private final Logger LOG = LoggerFactory.getLogger(MPMCRunner.class);

    private final EventLogger<String> logger;
    private final int producerCount;
    private final int consumerCount;
    private final int msgNum;
    private long nextId;
    private final BiConsumer<IdAndOffset, Throwable> handler = new BiConsumer<IdAndOffset, Throwable>() {
        @Override
        public void accept(IdAndOffset idAndOffset, Throwable e) {
            if (e != null) {
                LOG.error("Producer error", e);
            } else if (nextId != idAndOffset.id()) {
                LOG.error("id check failed", new RuntimeException());
            }
            nextId++;
        }
    };

    public MPMCRunner(EventLogger<String> logger, int producerCount, int consumerCount, int msgNum) {
        this.logger = logger;
        this.producerCount = producerCount;
        this.consumerCount = consumerCount;
        this.msgNum = msgNum;
        this.nextId = logger.nextId();
    }

    public void run() {
        ExecutorService producerExecutor = Executors.newFixedThreadPool(producerCount);
        ExecutorService consumerExecutor = Executors.newFixedThreadPool(consumerCount);

        LongAdder totalConsumed = new LongAdder();

        for (int i = 0; i < producerCount; i++) {
            producerExecutor.submit(new Producer());
        }

        for (int i = 0; i < consumerCount; i++) {
            consumerExecutor.submit(new Consumer(totalConsumed));
        }

        producerExecutor.shutdown();
        try {
            producerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        consumerExecutor.shutdown();
        try {
            consumerExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("Total consumed: " + totalConsumed);
    }

    private class Producer implements Runnable {
        private final Random random = new Random();
        private final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

        @Override
        public void run() {
            CompletableFuture<?> f = null;
            for (int i = 0; i < msgNum; i++) {
                String message = generateRandomString(random.nextInt(200));
                f = logger.write(message).whenComplete(handler);
            }
            Objects.requireNonNull(f).join();
            System.out.println("Finished id: " + nextId);
        }

        private String generateRandomString(int length) {
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        }
    }

    private class Consumer implements Runnable {
        private final LongAdder adder;

        public Consumer(LongAdder adder) {
            this.adder = adder;
        }

        @Override
        public void run() {
            IdAndOffset idAndOffset = new IdAndOffset(0, 0);
            while (true) {
                CompletableFuture<IdAndOffset> future = new CompletableFuture<>();
                long id = idAndOffset.id();
                LogObserver<String> observer = new LogObserver<>() {
                    private long expectId = id;

                    @Override
                    public void onNext(long id, long offset, String value) {
                        adder.add(1);
                        if (expectId != id) {
                            LOG.error("id check failed", new RuntimeException());
                        }
                        expectId++;
                    }

                    @Override
                    public void onComplete(long nextId, long nextOffset) {
                        if (expectId != nextId) {
                            LOG.error("id check failed", new RuntimeException());
                        }
                        future.complete(new IdAndOffset(nextId, nextOffset));
                    }

                    @Override
                    public void onError(Throwable e) {
                        future.completeExceptionally(e);
                    }
                };
                logger.read(idAndOffset, 100, observer);
                IdAndOffset joined = future.join();
                if (joined.equals(idAndOffset)) {
                    System.out.println(idAndOffset);
                    break;
                }
                idAndOffset = joined;
            }
        }
    }
}
