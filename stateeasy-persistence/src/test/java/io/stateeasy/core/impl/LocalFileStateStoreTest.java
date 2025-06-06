package io.stateeasy.core.impl;

import io.stateeasy.concurrent.EventStageListener;
import io.stateeasy.core.Snapshot;
import io.stateeasy.core.SnapshotAndId;
import io.stateeasy.core.StateDef;
import io.stateeasy.indexlogging.EventLogger;
import io.stateeasy.indexlogging.IdAndOffset;
import io.stateeasy.indexlogging.LogSystem;
import io.stateeasy.indexlogging.Serializer;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class LocalFileStateStoreTest {

    @Mock
    private LogFileEventStoreConfig config;
    @Mock
    private Serializer<String> serializer;
    @Mock
    private EventLogger<Snapshot<String>> logger;
    @Mock
    private LogSystem logSystem;
    @Mock
    private StateDef<String, ?> stateDef;
    @Mock
    private EventStageListener<Void> initListener;
    @Mock
    private EventStageListener<Long> writeListener;
    @Mock
    private EventStageListener<SnapshotAndId<String>> readListener;
    @Mock
    private EventStageListener<Boolean> expireListener;

    private LocalFileStateStore<String> stateStore;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(config.getThreadNumPerDisk()).thenReturn(1);
        when(config.toLogConfig(anyString())).thenReturn(null);
        when(stateDef.name()).thenReturn("testState");
        stateStore = new LocalFileStateStore<>(config, serializer);
    }

    @Test
    public void testInitialize() throws Exception {
        LocalFileLogSystemProvider.setLogSystem(logSystem);
        when(logSystem.<Snapshot<String>>get(any(), any())).thenReturn(logger);

        stateStore.initialize(stateDef, initListener);

        verify(initListener).success(null);
        assertNotNull(stateStore.logger);
    }

    @Test
    public void testWrite() {
        Snapshot<String> snapshot = new Snapshot<>("state", 1L, Map.of());
        stateStore.logger = logger;

        stateStore.write(snapshot, writeListener);

        ArgumentCaptor<EventStageListener<IdAndOffset>> captor =
                ArgumentCaptor.forClass(EventStageListener.class);
        verify(logger).write(eq(snapshot), eq(true), captor.capture());
        captor.getValue().success(new IdAndOffset(1, 2));

        verify(writeListener).success(1L);
    }

    @Test
    public void testWriteFailure() {
        Snapshot<String> snapshot = new Snapshot<>("state", 1L, Map.of());
        stateStore.logger = logger;

        stateStore.write(snapshot, writeListener);

        ArgumentCaptor<EventStageListener<IdAndOffset>> captor =
                ArgumentCaptor.forClass(EventStageListener.class);
        verify(logger).write(eq(snapshot), eq(true), captor.capture());
        captor.getValue().failure(new RuntimeException("Write failed"));

        verify(writeListener).failure(any(Throwable.class));
    }

    @Test
    public void testRead() {
        stateStore.logger = logger;
        when(logger.endId()).thenReturn(1L);

        stateStore.read(readListener);

        ArgumentCaptor<EventStageListener<Snapshot<String>>> captor =
                ArgumentCaptor.forClass(EventStageListener.class);
        verify(logger).readOne(eq(1L), captor.capture());
        captor.getValue().success(new Snapshot<>("state", 1L, Map.of()));

        verify(readListener).success(any(SnapshotAndId.class));
    }

    @Test
    public void testReadFailure() {
        stateStore.logger = logger;
        when(logger.endId()).thenReturn(1L);

        stateStore.read(readListener);

        ArgumentCaptor<EventStageListener<Snapshot<String>>> captor =
                ArgumentCaptor.forClass(EventStageListener.class);
        verify(logger).readOne(eq(1L), captor.capture());
        captor.getValue().failure(new RuntimeException("Read failed"));

        verify(readListener).failure(any(Throwable.class));
    }

    @Test
    public void testExpire() {
        stateStore.logger = logger;

        stateStore.expire(1L, expireListener);

        verify(logger).expire(eq(1L), eq(expireListener));
    }

    @Test
    public void testClose() throws IOException {
        stateStore.logger = logger;
        LocalFileLogSystemProvider.setLogSystem(logSystem);

        stateStore.close();

        verify(logSystem).closeLogger(logger);
        assertNull(stateStore.logger);
    }
}
