package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.impl.MemoryEventStore;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class TestEventStore<EVENT> implements EventStore<EVENT> {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();
    private final EventStoreAdaptor<EVENT> internal;
    private final EventStoreAdaptor<EVENT> memory;

    public TestEventStore(int capacity, EventStore<EVENT> internal) {
        memory = new EventStoreAdaptor<>(new MemoryEventStore<>(executor, capacity));
        this.internal = internal == null ? null : new EventStoreAdaptor<>(internal);
    }

    public TestEventStore() {
        this(1024, null);
    }

    @Override
    public void initialize(EventSourceStateDef<?, EVENT> stateDef,
                           EventStageListener<Void> listener) {
        var future = memory.initialize(stateDef, executor.newPromise());
        if (internal != null) {
            future = future
                    .flatmap(v -> internal.initialize(stateDef, executor.newPromise()));
        }
        future.addListener(listener);
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        var future = memory.append(event, executor.newPromise());
        if (internal != null) {
            future = future
                    .flatmap(h -> internal.append(h.event(), executor.newPromise())
                            .map(h2 -> {
                                if (!h2.equals(h)) {
                                    throw new RuntimeException("Event not equal");
                                }
                                return h2;
                            }));
        }
        future.addListener(listener);
    }

    @Override
    public void expire(long expireBeforeEventId, EventStageListener<Boolean> listener) {
        var future = memory.expire(expireBeforeEventId, executor.newPromise());
        if (internal != null) {
            future = future
                    .flatmap(b -> internal.expire(expireBeforeEventId, executor.newPromise()));
        }
        future.addListener(listener);
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        if (internal == null) {
            memory.recover(recoverAtEventId, observer);
            return;
        }
        var promise = executor.<Queue<EventHolder<EVENT>>>newPromise();
        memory.recover(recoverAtEventId, new EventObserver<>() {
            private final Queue<EventHolder<EVENT>> queue = new ConcurrentLinkedQueue<>();

            @Override
            public void onEvent(EventHolder<EVENT> event) {
                queue.add(event);
            }

            @Override
            public void onComplete() {
                promise.success(queue);
            }

            @Override
            public void onError(Throwable error) {
                promise.failure(error);
            }
        });
        promise.addListener(new EventStageListener<>() {
            @Override
            public void success(Queue<EventHolder<EVENT>> queue) {
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

            @Override
            public void failure(Throwable cause) {
                observer.onError(cause);
            }
        });
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        EventStage<Void> future = memory.flush(executor.newPromise());
        if (internal != null) {
            future = future.flatmap(v -> internal.flush(executor.newPromise()));
        }
        future.addListener(listener);
    }
}
