package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.SingleThreadEventExecutor;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyInt;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChunkedLogIndexerTest {

    @Test
    public void testAppendWithinChunkSize() {
        MappedByteBufferLogIndexer indexer = mock(MappedByteBufferLogIndexer.class);
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        ChunkedLogIndexer chunkedLogIndexer = new ChunkedLogIndexer(indexer, executor, 100, 50,
                Duration.ofSeconds(1));
        chunkedLogIndexer.append(1, 49);
        verify(indexer, never()).append(anyInt(), anyInt());
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testAppendExceedsChunkSizeButNotSyncSize() {
        MappedByteBufferLogIndexer indexer = mock(MappedByteBufferLogIndexer.class);
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        ChunkedLogIndexer chunkedLogIndexer = new ChunkedLogIndexer(indexer, executor, 100, 200,
                Duration.ofSeconds(1));
        chunkedLogIndexer.append(1, 150);
        verify(indexer).append(1, 150);
        verify(executor).schedule(any(Runnable.class), eq(1_000_000_000L),
                eq(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testAppendExceedsBothChunkAndSyncSize() {
        MappedByteBufferLogIndexer indexer = mock(MappedByteBufferLogIndexer.class);
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        ChunkedLogIndexer chunkedLogIndexer = new ChunkedLogIndexer(indexer, executor, 100, 50,
                Duration.ofSeconds(1));
        chunkedLogIndexer.append(1, 200);
        verify(indexer).append(1, 200);
        verify(indexer).persist();
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testAppendWithExistingSyncTask() {
        MappedByteBufferLogIndexer indexer = mock(MappedByteBufferLogIndexer.class);
        EventExecutor executor = mock(SingleThreadEventExecutor.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        ChunkedLogIndexer chunkedLogIndexer = new ChunkedLogIndexer(indexer, executor, 100, 50,
                Duration.ofSeconds(1));
        chunkedLogIndexer.append(1, 200);
        chunkedLogIndexer.append(2, 300);
        verify(indexer, times(2)).append(anyInt(), anyInt());
        verify(indexer, times(2)).persist();
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
}
