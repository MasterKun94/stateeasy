package io.masterkun.stateeasy.core.impl;

import io.masterkun.stateeasy.core.EventStore.EventHolder;
import io.masterkun.stateeasy.core.EventStore.EventObserver;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MemoryEventStoreTest {

    @Test
    public void testAppendSingleEvent() throws ExecutionException, InterruptedException {
        MemoryEventStore<String> store = new MemoryEventStore<>(10);
        CompletableFuture<EventHolder<String>> future = store.append("testEvent");
        EventHolder<String> result = future.get();
        assertNotNull(result);
        assertEquals(0, result.eventId());
        assertEquals("testEvent", result.event());
    }

    @Test
    public void testAppendMultipleEvents() throws ExecutionException, InterruptedException {
        MemoryEventStore<String> store = new MemoryEventStore<>(10);
        store.append("event1").get();
        store.append("event2").get();
        CompletableFuture<EventHolder<String>> future = store.append("event3");
        EventHolder<String> result = future.get();
        assertNotNull(result);
        assertEquals(2, result.eventId());
        assertEquals("event3", result.event());
    }

    @Test
    public void testAppendEventWithFullCapacity() throws ExecutionException, InterruptedException {
        MemoryEventStore<String> store = new MemoryEventStore<>(2);
        store.append("event1").get();
        store.append("event2").get();
        CompletableFuture<EventHolder<String>> future = store.append("event3");
        EventHolder<String> result = future.get();
        assertNotNull(result);
        assertEquals(2, result.eventId());
        assertEquals("event3", result.event());
    }

    @Test
    public void testRecoverWithinRange() throws ExecutionException, InterruptedException {
        MemoryEventStore<String> store = new MemoryEventStore<>(10);
        store.append("event1").get();
        store.append("event2").get();
        store.append("event3").get();

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
        MemoryEventStore<String> store = new MemoryEventStore<>(10);
        store.append("event1").get();
        store.append("event2").get();
        store.append("event3").get();

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
