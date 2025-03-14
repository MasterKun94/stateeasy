package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.concurrent.DefaultSingleThreadEventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventFuture;
import io.masterkun.stateeasy.core.EventStore.EventHolder;
import io.masterkun.stateeasy.core.EventStore.EventObserver;
import io.masterkun.stateeasy.core.EventStoreAdaptor;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MemoryEventStoreTest {
    private static final EventExecutor executor = new DefaultSingleThreadEventExecutor();

    @Test
    public void testAppendSingleEvent() throws ExecutionException, InterruptedException {
        EventStoreAdaptor<String> store = new EventStoreAdaptor<>(new MemoryEventStore<>(executor
                , 10));
        EventHolder<String> result =
                store.append("testEvent", executor.newPromise()).toFuture().get();
        assertNotNull(result);
        assertEquals(0, result.eventId());
        assertEquals("testEvent", result.event());
    }

    @Test
    public void testAppendMultipleEvents() throws ExecutionException, InterruptedException {
        EventStoreAdaptor<String> store = new EventStoreAdaptor<>(new MemoryEventStore<>(executor
                , 10));
        store.append("event1", executor.newPromise()).toFuture().get();
        store.append("event2", executor.newPromise()).toFuture().get();
        EventFuture<EventHolder<String>> future =
                store.append("event3", executor.newPromise()).toFuture();
        EventHolder<String> result = future.get();
        assertNotNull(result);
        assertEquals(2, result.eventId());
        assertEquals("event3", result.event());
    }

    @Test
    public void testAppendEventWithFullCapacity() throws ExecutionException, InterruptedException {
        EventStoreAdaptor<String> store = new EventStoreAdaptor<>(new MemoryEventStore<>(executor
                , 10));
        store.append("event1", executor.newPromise()).toFuture().get();
        store.append("event2", executor.newPromise()).toFuture().get();
        EventFuture<EventHolder<String>> future =
                store.append("event3", executor.newPromise()).toFuture();
        EventHolder<String> result = future.get();
        assertNotNull(result);
        assertEquals(2, result.eventId());
        assertEquals("event3", result.event());
    }

    @Test
    public void testRecoverWithinRange() throws ExecutionException, InterruptedException {
        EventStoreAdaptor<String> store = new EventStoreAdaptor<>(new MemoryEventStore<>(executor
                , 10));
        store.append("event1", executor.newPromise()).toFuture().get();
        store.append("event2", executor.newPromise()).toFuture().get();
        store.append("event3", executor.newPromise()).toFuture().get();

        CompletableFuture<List<EventHolder<String>>> future = new CompletableFuture<>();
        store.recover(1, new EventObserver<>() {
            List<EventHolder<String>> list = new ArrayList<>();

            @Override
            public void onEvent(EventHolder<String> event) {
                list.add(event);
            }

            @Override
            public void onComplete() {
                future.complete(list);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        var recoveredEvents = future.join();
        assertEquals(2, recoveredEvents.size());
        assertEquals(1, recoveredEvents.get(0).eventId());
        assertEquals("event2", recoveredEvents.get(0).event());
        assertEquals(2, recoveredEvents.get(1).eventId());
        assertEquals("event3", recoveredEvents.get(1).event());
    }

    @Test
    public void testRecoverOutOfRange() throws ExecutionException, InterruptedException {
        EventStoreAdaptor<String> store = new EventStoreAdaptor<>(new MemoryEventStore<>(executor
                , 10));
        store.append("event1", executor.newPromise()).toFuture().get();
        store.append("event2", executor.newPromise()).toFuture().get();
        store.append("event3", executor.newPromise()).toFuture().get();

        CompletableFuture<List<EventHolder<String>>> future = new CompletableFuture<>();
        store.recover(0, new EventObserver<String>() {
            private List<EventHolder<String>> list = new ArrayList<>();

            @Override
            public void onEvent(EventHolder<String> event) {
                list.add(event);
            }

            @Override
            public void onComplete() {
                future.complete(list);
            }

            @Override
            public void onError(Throwable error) {
                future.completeExceptionally(error);
            }
        });
        List<EventHolder<String>> join = future.join();
        assertEquals(3, join.size());
        assertEquals(new EventHolder<>(0, "event1"), join.get(0));
        assertEquals(new EventHolder<>(1, "event2"), join.get(1));
        assertEquals(new EventHolder<>(2, "event3"), join.get(2));
    }
}
