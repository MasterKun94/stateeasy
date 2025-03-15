package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventPromise;
import io.masterkun.stateeasy.concurrent.EventStage;
import io.masterkun.stateeasy.concurrent.EventStageListener;

/**
 * An adaptor for the {@link EventStore} interface, providing a way to wrap another implementation
 * of the {@code EventStore} and potentially add additional behavior or modifications to its
 * methods. This class delegates all calls to the wrapped {@code EventStore} instance, allowing for
 * extension points around the core functionality.
 *
 * @param <EVENT> the type of events managed by this store
 */
public class EventStoreAdaptor<EVENT> implements EventStore<EVENT> {
    private final EventStore<EVENT> delegate;

    public EventStoreAdaptor(EventStore<EVENT> delegate) {
        this.delegate = delegate;
    }

    public EventStage<Void> flush(EventPromise<Void> promise) {
        flush((EventStageListener<Void>) promise);
        return promise;
    }

    public EventStage<Void> initialize(EventSourceStateDef<?, EVENT> stateDef,
                                       EventPromise<Void> promise) {
        initialize(stateDef, (EventStageListener<Void>) promise);
        return promise;
    }

    @Override
    public void initialize(EventSourceStateDef<?, EVENT> stateDef,
                           EventStageListener<Void> listener) {
        delegate.initialize(stateDef, listener);
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        delegate.flush(listener);
    }

    public EventStage<EventHolder<EVENT>> append(EVENT event,
                                                 EventPromise<EventHolder<EVENT>> promise) {
        append(event, (EventStageListener<EventHolder<EVENT>>) promise);
        return promise;
    }

    public EventStage<Boolean> expire(long expireAtEventId, EventPromise<Boolean> promise) {
        expire(expireAtEventId, (EventStageListener<Boolean>) promise);
        return promise;
    }

    @Override
    public void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener) {
        delegate.append(event, listener);
    }

    @Override
    public void expire(long expireBeforeEventId, EventStageListener<Boolean> listener) {
        delegate.expire(expireBeforeEventId, listener);
    }

    @Override
    public void recover(long recoverAtEventId, EventObserver<EVENT> observer) {
        delegate.recover(recoverAtEventId, observer);
    }

}
