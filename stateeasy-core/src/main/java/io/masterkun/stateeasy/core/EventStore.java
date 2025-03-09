package io.masterkun.stateeasy.core;

import java.util.concurrent.CompletableFuture;

public interface EventStore<EVENT> {
    CompletableFuture<Void> flush();

    CompletableFuture<EventHolder<EVENT>> append(EVENT event);

    void recover(long recoverAtEventId, EventObserver<EVENT> observer);

    record EventHolder<EVENT>(long eventId, EVENT event) {
    }

    interface EventObserver<EVENT> {
        void onEvent(EventHolder<EVENT> event);
        void onComplete();
        void onError(Throwable error);
    }
}
