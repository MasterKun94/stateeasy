package io.masterkun.stateeasy.core;

import io.masterkun.stateeasy.concurrent.EventStage;

import java.util.function.Function;

public interface StateManager<STATE, MSG> {
    EventStage<Void> start();

    EventStage<Void> send(MSG msg);

    <T> EventStage<T> sendAndQuery(MSG msg, Function<STATE, T> function);

    <T> EventStage<T> queryFast(Function<STATE, T> function);

    <T> EventStage<T> query(Function<STATE, T> function);

    EventStage<Void> shutdown();
}
