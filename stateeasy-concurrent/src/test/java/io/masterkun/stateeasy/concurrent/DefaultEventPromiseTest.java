package io.masterkun.stateeasy.concurrent;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DefaultEventPromiseTest {

    @Test
    public void testSuccessWithExecutorInExecutor() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);

        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.success("value");

        assertEquals(1, promise.status);
        assertEquals("value", promise.obj);
    }

    @Test
    public void testSuccessWithExecutorNotInExecutor() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(false);

        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.success("value");

        verify(executor).execute(any(Runnable.class));
    }

    @Test
    public void testDoSuccessWithListeners() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);

        EventStageListener<String> listener = mock(EventStageListener.class);
        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.listeners = Collections.singletonList(listener);

        promise.success("value");

        assertEquals(1, promise.status);
        assertEquals("value", promise.obj);
        verify(listener).success("value");
    }

    @Test
    public void testDoSuccessWithoutListeners() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);

        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.listeners = null;

        promise.success("value");

        assertEquals(1, promise.status);
        assertEquals("value", promise.obj);
    }

    @Test
    public void testSuccessMethodWithExistingListeners() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);

        EventStageListener<String> listener = mock(EventStageListener.class);
        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.listeners = Collections.singletonList(listener);

        promise.success("value");

        assertEquals(1, promise.status);
        assertEquals("value", promise.obj);
        verify(listener).success("value");
    }

    @Test
    public void testSuccessMethodWithNoListeners() {
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(executor.inExecutor()).thenReturn(true);

        DefaultEventPromise<String> promise = new DefaultEventPromise<>(executor);
        promise.listeners = null;

        promise.success("value");

        assertEquals(1, promise.status);
        assertEquals("value", promise.obj);
    }
}
