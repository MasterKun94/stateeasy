package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventStageListener;

public interface EventStore<EVENT> {

    void initialize(EventSourceStateDef<?, EVENT> stateDef, EventStageListener<Void> listener);

    void flush(EventStageListener<Void> listener);

    void append(EVENT event, EventStageListener<EventHolder<EVENT>> listener);

    void expire(long expireAtEventId, EventStageListener<Boolean> listener);

    void recover(long recoverAtEventId, EventObserver<EVENT> observer);

    interface EventObserver<EVENT> {
        void onEvent(EventHolder<EVENT> event);

        void onComplete();

        void onError(Throwable error);
    }

    record EventHolder<EVENT>(long eventId, EVENT event) {
    }
}
