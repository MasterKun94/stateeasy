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

public abstract class AbstractStateManager<STATE, EVENT, STATE_DEF extends StateDef<STATE, EVENT>>
        implements StateManager<STATE, EVENT> {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractStateManager.class);

    protected final STATE_DEF stateDef;
    private final EventExecutor executor;
    protected STATE state;
    private EventStoreAdaptor<EVENT> eventStore;
    private StateStoreAdaptor<STATE> stateStore;
    private ScheduledFuture<?> snapshotTask;
    private long snapshotId;
    private long eventId;
    private long lastSnapshotEventId = -1;

    AbstractStateManager(EventExecutor singleThreadExecutor,
                         STATE_DEF stateDef) {
        this.executor = singleThreadExecutor;
        this.stateDef = stateDef;

    }

    @Override
    public EventStage<Void> start() {
        long interval = stateDef.snapshotConfig().snapshotInterval().toMillis();
        this.snapshotTask = executor.scheduleWithFixedDelay(this::snapshot,
                interval, interval, TimeUnit.MILLISECONDS);
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
                    afterStart();
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

    protected abstract void afterStart();

    protected abstract STATE internalGetState();

    protected abstract void internalUpdate(EVENT event);

    private void snapshot() {
        if (lastSnapshotEventId == eventId) {
            return;
        }
        try {
            Snapshot<STATE> snapshot = new Snapshot<>(snapshotId, state, eventId, Map.of());
            EventStage<Void> stage = stateStore.write(snapshot, executor.newPromise());
            if (stateDef.snapshotConfig().autoExpire()) {
                stage = stage
                        .flatmap(v -> stateStore.expire(snapshotId, executor.newPromise()))
                        .flatmap(b -> eventStore.expire(eventId, executor.newPromise()))
                        .map(b -> null);
            }
            stage.addListener(new EventStageListener<>() {
                @Override
                public void success(Void value) {

                }

                @Override
                public void failure(Throwable cause) {
                    // TODO failure handler
                }
            });
            lastSnapshotEventId = eventId;
            snapshotId++;
        } catch (Exception e) {
            LOG.error("{} snapshot failed with id {}", stateDef, snapshotId, e);
        }
    }

    @Override
    public EventStage<Void> send(EVENT event) {
        return eventStore.append(event, executor.newPromise())
                .map(holder -> {
                    internalUpdate(event);
                    eventId = holder.eventId();
                    return null;
                });
    }

    @Override
    public <T> EventStage<T> sendAndQuery(EVENT event, Function<STATE, T> function) {
        return eventStore.append(event, executor.newPromise())
                .map(holder -> {
                    internalUpdate(event);
                    eventId = holder.eventId();
                    return function.apply(internalGetState());
                });
    }

    @Override
    public <T> EventStage<T> queryFast(Function<STATE, T> function) {
        return EventStage.supplyAsync(() -> function.apply(internalGetState()), executor);
    }

    @Override
    public <T> EventStage<T> query(Function<STATE, T> function) {
        return eventStore.flush(executor.newPromise())
                .map(v -> function.apply(internalGetState()));
    }

    @Override
    public EventStage<Void> shutdown() {
        return EventStage.runAsync(() -> {
            try {
                snapshotTask.cancel(false);
                snapshot();
            } finally {
                stateStore.close();
            }
        }, executor);
    }
}
