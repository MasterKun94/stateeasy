package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer;
import io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer;
import org.junit.Test;

import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
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
    public void testUpdateWithinChunkSize() {
        io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer indexer = mock(io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer chunkedLogIndexer = new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(indexer, executor, 100, 50, Duration.ofSeconds(1));
        chunkedLogIndexer.update(1, 49);
        verify(indexer, never()).update(anyInt(), anyInt());
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testUpdateExceedsChunkSizeButNotSyncSize() {
        io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer indexer = mock(io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer chunkedLogIndexer = new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(indexer, executor, 100, 200, Duration.ofSeconds(1));
        chunkedLogIndexer.update(1, 150);
        verify(indexer).update(1, 150);
        verify(executor).schedule(any(Runnable.class), eq(1_000_000_000L), eq(TimeUnit.NANOSECONDS));
    }

    @Test
    public void testUpdateExceedsBothChunkAndSyncSize() {
        io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer indexer = mock(io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer chunkedLogIndexer = new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(indexer, executor, 100, 50, Duration.ofSeconds(1));
        chunkedLogIndexer.update(1, 200);
        verify(indexer).update(1, 200);
        verify(indexer).persist();
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testUpdateWithExistingSyncTask() {
        io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer indexer = mock(io.masterkun.stateeasy.indexlogging.impl.MappedByteBufferLogIndexer.class);
        ScheduledExecutorService executor = mock(ScheduledExecutorService.class);
        when(indexer.endOffset()).thenReturn(0);
        when(indexer.endId()).thenReturn(0);
        when(indexer.offsetBefore(anyInt())).thenReturn(0);
        io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer chunkedLogIndexer = new io.masterkun.stateeasy.indexlogging.impl.ChunkedLogIndexer(indexer, executor, 100, 50, Duration.ofSeconds(1));
        chunkedLogIndexer.update(1, 200);
        chunkedLogIndexer.update(2, 300);
        verify(indexer, times(2)).update(anyInt(), anyInt());
        verify(indexer, times(2)).persist();
        verify(executor, never()).schedule(any(Runnable.class), anyLong(), any(TimeUnit.class));
    }
}
