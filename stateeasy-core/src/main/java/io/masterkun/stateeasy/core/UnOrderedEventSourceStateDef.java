package io.masterkun.stateeasy.core;

public interface UnOrderedEventSourceStateDef<STATE, EVENT> extends StateDef<STATE, EVENT>,
        EventSourceStateDef<STATE, EVENT> {
}
