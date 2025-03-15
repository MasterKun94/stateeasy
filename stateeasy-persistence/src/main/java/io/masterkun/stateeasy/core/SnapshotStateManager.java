package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.core.impl.NoopEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

public class SnapshotStateManager<STATE, EVENT, STATE_DEF extends StateDef<STATE, EVENT>>
        implements StateManager<STATE, EVENT> {
    private static final Logger LOG = LoggerFactory.getLogger(SnapshotStateManager.class);

    protected final STATE_DEF stateDef;
    private final EventExecutor executor;
    protected STATE state;
    private long snapshotInterval;
    private long snapshotMsgMax;
    private EventStoreAdaptor<EVENT> eventStore;
    private StateStoreAdaptor<STATE> stateStore;
    private ScheduledFuture<?> snapshotTask;
    private boolean snapshotRunning;
    private long snapshotId;
    private long eventId;
    private long lastSnapshotEventId = -1;

    SnapshotStateManager(EventExecutor singleThreadExecutor,
                         STATE_DEF stateDef) {
        this.executor = singleThreadExecutor;
        this.stateDef = stateDef;
    }

    @Override
    public EventStage<Void> start() {
        snapshotInterval = stateDef.snapshotConfig().getSnapshotInterval().toMillis();
        snapshotMsgMax = stateDef.snapshotConfig().getSnapshotMsgMax();
        this.stateStore = new StateStoreAdaptor<>(stateDef.stateStore(executor));
        EventStage<Void> initFuture;
        if (stateDef instanceof EventSourceStateDef) {
            @SuppressWarnings("unchecked")
            var eventSourceStateDef = (EventSourceStateDef<?, EVENT>) stateDef;
            this.eventStore = new EventStoreAdaptor<>(eventSourceStateDef.eventStore(executor));
            initFuture = this.eventStore.initialize(eventSourceStateDef, executor.newPromise());
        } else {
            this.eventStore = new EventStoreAdaptor<>(new NoopEventStore<>());
            initFuture = EventStage.succeed(null, executor);
        }

        return initFuture.flatmap(v -> stateStore.initialize(stateDef, executor.newPromise()))
                .flatmap(v -> stateStore.read(executor.newPromise()))
                .flatmap(read -> {
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
                    EventPromise<Void> promise = executor.newPromise();
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
                            promise.success(null);
                        }

                        @Override
                        public void onError(Throwable error) {
                            promise.failure(error);
                        }
                    });
                    return promise;
                }, executor);
    }

    protected void internalUpdate(EVENT event) {
        state = stateDef.update(state, event);
    }

    private void snapshot() {
        assert executor.inExecutor();
        snapshotTask = null;
        if (lastSnapshotEventId == eventId) {
            return;
        }
        try {
            snapshotRunning = true;
            Snapshot<STATE> snapshot = new Snapshot<>(state, eventId, Map.of());
            EventStage<Long> stage = stateStore.write(snapshot, executor.newPromise())
                    .addListener(new EventStageListener<>() {
                        @Override
                        public void success(Long value) {
                            snapshotRunning = false;
                            SnapshotStateManager.this.snapshotId = value;
                            SnapshotStateManager.this.lastSnapshotEventId = eventId;
                        }

                        @Override
                        public void failure(Throwable cause) {
                            snapshotRunning = false;
                            LOG.error("{} snapshot failed, latest snapshot id is {}",
                                    stateDef, lastSnapshotEventId, cause);
                        }
                    });
            if (stateDef.snapshotConfig().isAutoExpire()) {
                stage.flatmap(sId -> stateStore.expire(sId, executor.newPromise()))
                        .flatmap(b -> eventStore.expire(eventId, executor.newPromise()))
                        .addListener(new EventStageListener<>() {
                            @Override
                            public void success(Boolean value) {
                                // TODO
                            }

                            @Override
                            public void failure(Throwable cause) {
                                LOG.error("{} expire failed with id {}", stateDef, snapshotId,
                                        cause);
                            }
                        });
            }
        } catch (Exception e) {
            snapshotRunning = false;
            LOG.error("Unexpected error", e);
        }
    }

    @Override
    public EventStage<Void> send(EVENT event) {
        return eventStore.append(event, executor.newPromise())
                .map(holder -> {
                    internalUpdate(event);
                    eventId = holder.eventId();
                    if (!snapshotRunning && eventId - lastSnapshotEventId > snapshotMsgMax) {
                        if (snapshotTask != null) {
                            snapshotTask.cancel(false);
                        }
                        snapshot();
                    } else if (snapshotTask == null) {
                        snapshotTask = executor.schedule(this::snapshot,
                                snapshotInterval, TimeUnit.MILLISECONDS);
                    }
                    return null;
                });
    }

    @Override
    public <T> EventStage<T> sendAndQuery(EVENT event, Function<STATE, T> function) {
        return send(event).map(v -> function.apply(state));
    }

    @Override
    public <T> EventStage<T> queryFast(Function<STATE, T> function) {
        return EventStage.supplyAsync(() -> function.apply(state), executor);
    }

    @Override
    public <T> EventStage<T> query(Function<STATE, T> function) {
        return eventStore.flush(executor.newPromise())
                .map(v -> function.apply(state));
    }

    @Override
    public EventStage<Void> shutdown() {
        return EventStage.runAsync(() -> {
            try {
                if (snapshotTask != null) {
                    snapshotTask.cancel(false);
                }
                snapshot();
            } finally {
                stateStore.close();
            }
        }, executor);
    }
}
