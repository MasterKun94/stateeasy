package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.core.impl.MemoryEventStore;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestEventStore<EVENT> extends MemoryEventStore<EVENT> {
    private final EventStore<EVENT> internal;

    public TestEventStore(int capacity, EventStore<EVENT> internal) {
        super(capacity);
        this.internal = internal;
    }

    public TestEventStore() {
        super(1024);
        this.internal = null;
    }

    @Override
    public CompletableFuture<EventHolder<EVENT>> append(EVENT event) {
        var future = super.append(event);
        if (internal != null) {
            future = future.thenCompose(h -> internal.append(h.event())
                    .thenApply(h2 -> {
                        if (!h2.equals(h)) {
                            throw new RuntimeException("Event not equal");
                        }
                        return h2;
                    }));
        }
        return future;
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        if (internal == null) {
            super.recover(recoverAtEventId, observer);
            return;
        }
        CompletableFuture<Queue<EventHolder<EVENT>>> future = new CompletableFuture<>();
        super.recover(recoverAtEventId, new EventObserver<>() {
            private final Queue<EventHolder<EVENT>> queue = new ConcurrentLinkedQueue<>();

            @Override
            public void onEvent(EventHolder<EVENT> event) {
                queue.add(event);
            }

            @Override
            public void onComplete() {
                future.complete(queue);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        future.whenComplete((queue, e) -> {
            if (e != null) {
                observer.onError(e);
            } else {
                internal.recover(recoverAtEventId, new EventObserver<>() {
                    @Override
                    public void onEvent(EventHolder<EVENT> event) {
                        if (Objects.equals(queue.poll(), event)) {
                            observer.onEvent(event);
                        } else {
                            observer.onError(new RuntimeException("event check failed"));
                        }
                        throw new RuntimeException("event check failed");
                    }

                    @Override
                    public void onComplete() {
                        if (queue.isEmpty()) {
                            observer.onComplete();
                        } else {
                            observer.onError(new RuntimeException("event check failed"));
                        }
                    }

                    @Override
                    public void onError(Throwable error) {
                        observer.onError(error);
                    }
                });
            }
        });
    }

    @Override
    public CompletableFuture<Void> flush() {
        var future = super.flush();
        if (internal != null) {
            future = future.thenCompose(v -> internal.flush());
        }
        return future;
    }
}
