package io.masterkun.stateeasy.core;

import java.util.concurrent.ScheduledExecutorService;

public interface EventSourceStateDef<STATE, EVENT> extends StateDef<STATE, EVENT> {
    EventStore<EVENT> eventStore(ScheduledExecutorService executor);
}
