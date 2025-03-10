package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.core.impl.NoopEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public abstract class AbstractStateManager<STATE, EVENT, STATE_DEF extends StateDef<STATE, EVENT>>
        implements StateManager<STATE, EVENT> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStateManager.class);

    protected final STATE_DEF stateDef;
    private final ScheduledExecutorService executor;
    private final EventStore<EVENT> eventStore;
    private final StateStore<STATE> stateStore;
    protected STATE state;
    private ScheduledFuture<?> snapshotTask;
    private long snapshotId;
    private long eventId;
    private long lastSnapshotEventId = -1;

    AbstractStateManager(ScheduledExecutorService singleThreadExecutor,
                         STATE_DEF stateDef) {
        this.executor = singleThreadExecutor;
        this.stateDef = stateDef;
        this.stateStore = stateDef.stateStore(executor);
        if (stateDef instanceof EventSourceStateDef) {
            //noinspection unchecked
            this.eventStore = ((EventSourceStateDef<?, EVENT>) stateDef).eventStore(executor);
        } else {
            this.eventStore = new NoopEventStore<>();
        }
    }

    @Override
    public CompletableFuture<Void> start() {
        long interval = stateDef.snapshotConfig().snapshotInterval().toMillis();
        this.snapshotTask = executor.scheduleWithFixedDelay(this::snapshot,
                interval, interval, TimeUnit.MILLISECONDS);
        return stateStore.initialize(stateDef)
                .thenComposeAsync(v -> stateStore.read(), executor)
                .thenComposeAsync(read -> {
                    if (read == null) {
                        this.state = stateDef.initialState();
                        this.eventId = -1;
                        this.snapshotId = 0;
                    } else {
                        this.state = read.state();
                        this.eventId = read.eventId();
                        this.snapshotId = read.snapshotId();
                    }
                    this.lastSnapshotEventId = eventId;
                    afterStart();
                    CompletableFuture<Void> future = new CompletableFuture<>();
                    eventStore.recover(eventId + 1, new EventStore.EventObserver<>() {
                        @Override
                        public void onEvent(EventStore.EventHolder<EVENT> holder) {
                            executor.execute(() -> {
                                internalUpdate(holder.event());
                                eventId = holder.eventId();
                            });
                        }

                        @Override
                        public void onComplete() {
                            future.complete(null);
                        }

                        @Override
                        public void onError(Throwable error) {
                            future.completeExceptionally(error);
                        }
                    });
                    return future;
                }, executor);
    }

    protected abstract void afterStart();

    protected abstract STATE internalGetState();

    protected abstract void internalUpdate(EVENT event);

    private void snapshot() {
        if (lastSnapshotEventId == eventId) {
            return;
        }
        try {
            stateStore.write(new Snapshot<>(snapshotId, state, eventId, Map.of()));
            lastSnapshotEventId = eventId;
            snapshotId++;
        } catch (Exception e) {
            LOG.error("{} snapshot failed with id {}", stateDef, snapshotId, e);
        }
    }

    @Override
    public CompletableFuture<Void> send(EVENT event) {
        return eventStore
                .append(event)
                .thenAcceptAsync(holder -> {
                    internalUpdate(event);
                    eventId = holder.eventId();
                }, executor);
    }

    @Override
    public <T> CompletableFuture<T> sendAndQuery(EVENT event, Function<STATE, T> function) {
        return eventStore
                .append(event)
                .thenApplyAsync(holder -> {
                    internalUpdate(event);
                    eventId = holder.eventId();
                    return function.apply(internalGetState());
                }, executor);
    }

    @Override
    public <T> CompletableFuture<T> queryFast(Function<STATE, T> function) {
        return CompletableFuture
                .supplyAsync(() -> function.apply(internalGetState()), executor);
    }

    @Override
    public <T> CompletableFuture<T> query(Function<STATE, T> function) {
        return eventStore
                .flush()
                .thenApplyAsync(v -> function.apply(internalGetState()), executor);
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            try {
                snapshotTask.cancel(false);
                snapshot();
            } finally {
                stateStore.close();
            }
        }, executor);
    }
}
