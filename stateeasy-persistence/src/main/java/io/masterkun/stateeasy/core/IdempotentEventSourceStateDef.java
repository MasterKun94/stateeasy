package io.masterkun.stateeasy.core;

public interface IdempotentEventSourceStateDef<STATE, EVENT> extends StateDef<STATE, EVENT>,
        EventSourceStateDef<STATE, EVENT> {
}
