package io.masterkun.stateeasy.indexlogging;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventExecutorThreadFactory;
import io.masterkun.stateeasy.concurrent.SingleThreadEventExecutor;
import io.masterkun.stateeasy.indexlogging.executor.EventExecutorFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExecutorPoolTest {

    private EventExecutorFactory mockExecutorFactory;
    private EventExecutorThreadFactory mockThreadFactory;
    private File mockFile;
    private FileStore mockFileStore;

    @Before
    public void setUp() throws IOException {
        mockExecutorFactory = Mockito.mock(EventExecutorFactory.class);
        mockThreadFactory = Mockito.mock(EventExecutorThreadFactory.class);
        mockFile = Mockito.mock(File.class);
        var mockPath = Mockito.mock(Path.class);
        var mockFs = Mockito.mock(FileSystem.class);
        var mockFsProvider = Mockito.mock(FileSystemProvider.class);
        mockFileStore = Mockito.mock(FileStore.class);

        when(mockFile.exists()).thenReturn(true);
        when(mockFile.getParentFile()).thenReturn(mockFile);
        when(mockFile.getAbsoluteFile()).thenReturn(mockFile);
        when(mockFile.isAbsolute()).thenReturn(true);
        when(mockFile.toPath()).thenReturn(mockPath);
        when(mockPath.getFileSystem()).thenReturn(mockFs);
        when(mockFs.provider()).thenReturn(mockFsProvider);
        when(mockFsProvider.getFileStore(any())).thenReturn(mockFileStore);
        when(mockFileStore.name()).thenReturn("mockFileStore");
        when(mockExecutorFactory.createEventExecutor(mockThreadFactory))
                .thenAnswer(invocation -> mock(SingleThreadEventExecutor.class));
    }

    @Test
    public void testGetOrCreateExecutorWhenNotShutdown() {
        ExecutorPool executorPool = new ExecutorPool(1, mockThreadFactory, mockExecutorFactory);
        EventExecutor executor = executorPool.getOrCreate(mockFile);

        assertNotNull(executor);
        verify(mockExecutorFactory).createEventExecutor(mockThreadFactory);
    }

    @Test(expected = RuntimeException.class)
    public void testGetOrCreateExecutorWhenAlreadyShutdown() throws Exception {
        ExecutorPool executorPool = new ExecutorPool(1, mockThreadFactory, mockExecutorFactory);
        executorPool.shutdown().get();

        executorPool.getOrCreate(mockFile);
    }

    @Test
    public void testGetOrCreateExecutorWithSingleCalls() {
        ExecutorPool executorPool = new ExecutorPool(1, mockThreadFactory, mockExecutorFactory);
        when(mockFileStore.name()).thenReturn("mockFileStore1");
        EventExecutor executor1 = executorPool.getOrCreate(mockFile);
        EventExecutor executor2 = executorPool.getOrCreate(mockFile);

        assertNotNull(executor1);
        assertNotNull(executor2);
        assertEquals(executor1, executor2);
        verify(mockExecutorFactory, times(1)).createEventExecutor(mockThreadFactory);
    }

    @Test
    public void testGetOrCreateExecutorWithMultipleCalls() {
        ExecutorPool executorPool = new ExecutorPool(2, mockThreadFactory, mockExecutorFactory);
        when(mockFileStore.name()).thenReturn("mockFileStore1");
        EventExecutor executor1 = executorPool.getOrCreate(mockFile);
        EventExecutor executor2 = executorPool.getOrCreate(mockFile);
        EventExecutor executor3 = executorPool.getOrCreate(mockFile);

        assertNotNull(executor1);
        assertNotNull(executor2);
        assertNotNull(executor3);
        assertTrue(executor1 == executor2 || executor2 == executor3 || executor1 == executor3);
        verify(mockExecutorFactory, times(2))
                .createEventExecutor(mockThreadFactory);
    }

    @Test
    public void testGetOrCreateExecutorWithDifferentFileStores() {
        ExecutorPool executorPool = new ExecutorPool(1, mockThreadFactory, mockExecutorFactory);

        when(mockFileStore.name()).thenReturn("mockFileStore1");
        EventExecutor executor1 = executorPool.getOrCreate(mockFile);

        when(mockFileStore.name()).thenReturn("mockFileStore2");
        EventExecutor executor2 = executorPool.getOrCreate(mockFile);

        assertNotNull(executor1);
        assertNotNull(executor2);
        assertNotEquals(executor1, executor2);
        verify(mockExecutorFactory, times(2))
                .createEventExecutor(mockThreadFactory);
    }
}
