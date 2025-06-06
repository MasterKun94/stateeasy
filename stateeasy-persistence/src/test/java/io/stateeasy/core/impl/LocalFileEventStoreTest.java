package io.stateeasy.core.impl;

import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.core.EventSourceStateDef;
import io.stateeasy.core.EventStore.EventHolder;
import io.stateeasy.indexlogging.EventLogger;
import io.stateeasy.indexlogging.IdAndOffset;
import io.stateeasy.indexlogging.LogSystem;
import io.stateeasy.indexlogging.Serializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalFileEventStoreTest {

    private LocalFileEventStore<String> eventStore;
    private LogFileEventStoreConfig config;
    private Serializer<String> serializer;
    private EventLogger<String> logger;
    private LogSystem logSystem;

    @Before
    public void setUp() throws Exception {
        config = mock(LogFileEventStoreConfig.class);
        serializer = mock(Serializer.class);
        logger = mock(EventLogger.class);
        logSystem = mock(LogSystem.class);

        when(config.getThreadNumPerDisk()).thenReturn(1);
        when(logSystem.<String>get(any(), any())).thenReturn(logger);

        eventStore = new LocalFileEventStore<>(config, serializer);
        LocalFileLogSystemProvider.setLogSystem(logSystem);
    }

    @Test
    public void testInitialize() {
        EventSourceStateDef<?, String> stateDef = mock(EventSourceStateDef.class);
        EventStageListener<Void> listener = mock(EventStageListener.class);

        when(stateDef.name()).thenReturn("test");

        eventStore.initialize(stateDef, listener);

        verify(listener).success(null);
        assertNotNull(eventStore.logger);
    }

    @Test
    public void testInitializeFailure() throws IOException {
        EventSourceStateDef<?, String> stateDef = mock(EventSourceStateDef.class);
        EventStageListener<Void> listener = mock(EventStageListener.class);

        when(stateDef.name()).thenReturn("test");
        when(logSystem.get(any(), any())).thenThrow(new RuntimeException("Initialization failed"));

        eventStore.initialize(stateDef, listener);

        verify(listener).failure(any(Throwable.class));
    }

    @Test
    public void testFlush() {
        EventStageListener<Void> listener = mock(EventStageListener.class);

        eventStore.logger = logger;
        eventStore.flush(listener);

        verify(logger).flush(listener);
    }

    @Test
    public void testAppend() {
        String event = "testEvent";
        EventStageListener<EventHolder<String>> listener = mock(EventStageListener.class);
        IdAndOffset idAndOffset = new IdAndOffset(1L, 0L);

        eventStore.logger = logger;
        doAnswer(invocation -> {
            EventStageListener<IdAndOffset> callback = invocation.getArgument(1);
            callback.success(idAndOffset);
            return null;
        }).when(logger).write(eq(event), any(EventStageListener.class));

        eventStore.append(event, listener);

        ArgumentCaptor<EventHolder<String>> captor = ArgumentCaptor.forClass(EventHolder.class);
        verify(listener).success(captor.capture());
        assertEquals(event, captor.getValue().event());
    }

    @Test
    public void testAppendFailure() {
        String event = "testEvent";
        EventStageListener<EventHolder<String>> listener = mock(EventStageListener.class);


        doAnswer(invocation -> {
            EventStageListener<IdAndOffset> callback = invocation.getArgument(1);
            callback.failure(new RuntimeException("Write failed"));
            return null;
        }).when(logger).write(eq(event), any(EventStageListener.class));
        eventStore.logger = logger;

        eventStore.append(event, listener);

        verify(listener).failure(any(Throwable.class));
    }

    @Test
    public void testExpire() {
        long expireBeforeEventId = 1L;
        EventStageListener<Boolean> listener = mock(EventStageListener.class);

        eventStore.logger = logger;
        eventStore.expire(expireBeforeEventId, listener);

        verify(logger).expire(expireBeforeEventId, listener);
    }

    @Test
    public void testClose() throws IOException {
        eventStore.logger = logger;
        eventStore.close();

        verify(logSystem).closeLogger(logger);
        assertNull(eventStore.logger);
    }
}
