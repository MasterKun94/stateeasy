package io.masterkun.stateeasy.core;

import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public interface StateManager<STATE, MSG> {
    CompletableFuture<Void> start();

    CompletableFuture<Void> send(MSG msg);

    <T> CompletableFuture<T> sendAndQuery(MSG msg, Function<STATE, T> function);

    <T> CompletableFuture<T> queryFast(Function<STATE, T> function);

    <T> CompletableFuture<T> query(Function<STATE, T> function);

    CompletableFuture<Void> shutdown();
}
