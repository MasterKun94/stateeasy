package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.EventLogger;
import io.masterkun.commons.indexlogging.FlushListener;
import io.masterkun.commons.indexlogging.HasMetrics;
import io.masterkun.commons.indexlogging.IdAndOffset;
import io.masterkun.commons.indexlogging.LogConfig;
import io.masterkun.commons.indexlogging.LogObserver;
import io.masterkun.commons.indexlogging.Serializer;
import io.masterkun.commons.indexlogging.exception.FileAlreadyLockedException;
import io.masterkun.commons.indexlogging.exception.IdExpiredException;
import io.masterkun.commons.indexlogging.exception.LogCorruptException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

/**
 * An implementation of the {@link EventLogger} interface that manages a series of log segments for storing and retrieving events.
 * This class provides functionality to write, read, and manage log segments, as well as register metrics and handle cleanup.
 * <p>
 * The log segments are managed in a deque, with new segments being added to the front. The current segment is where new writes occur,
 * and once it reaches its capacity, a new segment is created. Segments can be read from, and old segments are periodically cleaned up
 * based on the configuration.
 * <p>
 * This class is thread-safe and ensures that all operations are performed in a consistent manner.
 *
 * @param <T> the type of the event objects to be logged
 */
public final class EventLoggerImpl<T> implements EventLogger<T>, HasMetrics, Closeable {
    private static final Logger LOG = LoggerFactory.getLogger(EventLoggerImpl.class);

    private final String name;
    private final Deque<LogSegment<T>> segments = new ArrayDeque<>();
    private final LogConfig segmentConfig;
    private final Serializer<T> serializer;
    private final ScheduledExecutorService executor;
    private final Executor readerExecutor;
    private final FileLock dirLock;
    private final FileOutputStream lockStream;
    private LogSegment<T> currentSegment;
    private volatile boolean closed;

    public EventLoggerImpl(String name,
                           LogConfig config,
                           Serializer<T> serializer,
                           ScheduledExecutorService executor,
                           Executor readerExecutor) throws IOException {
        this.name = name;
        File logDir = config.logDir();
        // 获取目录锁
        File lockFile = new File(logDir, "logger-" + name + ".lock");
        try {
            this.lockStream = new FileOutputStream(lockFile);
            this.dirLock = lockStream.getChannel().tryLock();
            if (dirLock == null) {
                throw new FileAlreadyLockedException(lockFile);
            }
        } catch (IOException e) {
            closeLockResources();
            throw e;
        }
        this.segmentConfig = config;
        this.serializer = serializer;
        this.executor = executor;
        this.readerExecutor = readerExecutor == null ? executor : readerExecutor;
        List<Long> list;
        if (logDir.exists()) {
            File[] metaFiles = logDir.listFiles(file -> file.getName().endsWith(".meta"));
            list = Arrays.stream(Objects.requireNonNull(metaFiles))
                    .map(f -> Utils.extractInitId(config, f))
                    .sorted()
                    .toList();
        } else {
            list = Collections.emptyList();
        }
        if (list.isEmpty()) {
            currentSegment = LogSegment.create(config, 0, 0, executor, serializer);
            segments.addFirst(currentSegment);
        } else {
            for (int i = 0, l = list.size(); i < l; i++) {
                boolean end = i == l - 1;
                long initId = list.get(i);
                currentSegment = LogSegment.recover(config, initId, executor, serializer, !end);
                if (!segments.isEmpty()) {
                    LogSegment<T> prev = segments.getFirst();
                    if (prev.endId() != currentSegment.startId() - 1) {
                        throw new LogCorruptException(prev + " and " + currentSegment +
                                " segment id not continuous, prev: " + prev.endId() +
                                ", new: " + currentSegment.startId());
                    }
                    if (prev.endOffset() >= currentSegment.startOffset() - 12) {
                        throw new LogCorruptException(prev + " and " + currentSegment +
                                " offset not incremental");
                    }
                }
                segments.addFirst(currentSegment);
            }
        }
    }

    // 新增锁资源释放方法
    private void closeLockResources() {
        try {
            if (dirLock != null) {
                dirLock.release();
            }
        } catch (IOException e) {
            LOG.error("释放文件锁失败", e);
        }
        try {
            if (lockStream != null) {
                lockStream.close();
            }
        } catch (IOException e) {
            LOG.error("关闭锁文件流失败", e);
        }
    }

    @Override
    public long startId() {
        return segments.getLast().startId();
    }

    @Override
    public long endId() {
        return segments.getFirst().endId();
    }

    @Override
    public long nextId() {
        return segments.getFirst().nextId();
    }

    @Override
    public CompletableFuture<IdAndOffset> write(T obj, boolean flush, boolean immediateCallback) {
        CompletableFuture<IdAndOffset> future = new CompletableFuture<>();
        write(obj, flush, immediateCallback, new FlushListener() {
            @Override
            public void onReceive(IdAndOffset idAndOffset) {
                future.complete(idAndOffset);
            }

            @Override
            public void onError(Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    @Override
    public void write(T obj, boolean flush, boolean immediateCallback, FlushListener listener) {
        if (closed) {
            listener.onError(new RuntimeException("already closed"));
            return;
        }
        var callback = new CallbackAdaptor(listener, immediateCallback);
        executor.execute(() -> {
            if (closed) {
                callback.onError(new RuntimeException("already closed"));
                return;
            }
            if (!currentSegment.getWriter().put(obj, callback, flush)) {
                try {
                    LOG.info("IndexLogger[{}] creating new segment at id={}, offset={}",
                            name, currentSegment.endId(), currentSegment.endOffset());
                    currentSegment.setReadOnly();
                    assert currentSegment.endId() == currentSegment.nextId() - 1;
                    var newSegment = LogSegment.create(segmentConfig, currentSegment.nextId(),
                            currentSegment.nextOffset(), executor, serializer);
                    segments.addFirst(newSegment);
                    currentSegment = newSegment;
                    if (!currentSegment.getWriter().put(obj, callback, flush)) {
                        throw new IllegalArgumentException("should never happen");
                    }
                    executor.execute(this::segmentCleanup);
                } catch (Throwable e) {
                    callback.onError(e);
                }
            }
        });
    }

    @Override
    public void read(long startId, int limit, LogObserver<T> observer) {
        read(0, startId, limit, observer);
    }

    @Override
    public void read(long startOffset, long startId, int limit, LogObserver<T> observer) {
        if (closed) {
            observer.onError(new RuntimeException("already closed"));
            return;
        }
        for (LogSegment<T> segment : segments) {
            if (segment.startId() <= startId) {
                if (segment == currentSegment) {
                    readerExecutor.execute(() -> {
                        if (closed) {
                            observer.onError(new RuntimeException("already closed"));
                            return;
                        }
                        try {
                            var iter = segment.getReader().get(startOffset, startId, limit);
                            if (iter.hasNext()) {
                                do {
                                    var next = iter.next();
                                    observer.onNext(next.id(), next.offset(), next.value());
                                } while (iter.hasNext());
                                observer.onComplete(iter.nextId(), iter.nextOffset());
                            } else if (readerExecutor == executor) {
                                addReadListener(segment, startOffset, startId, limit, observer);
                            } else {
                                executor.execute(() -> addReadListener(segment, startOffset, startId, limit, observer));
                            }
                        } catch (Throwable e) {
                            observer.onError(e);
                        }
                    });
                    return;
                } else {
                    readerExecutor.execute(() -> {
                        if (closed) {
                            observer.onError(new RuntimeException("already closed"));
                            return;
                        }
                        try {
                            var iter = segment.getReader().get(startOffset, startId, limit);
                            assert iter.hasNext();
                            while (iter.hasNext()) {
                                var next = iter.next();
                                observer.onNext(next.id(), next.offset(), next.value());
                            }
                            observer.onComplete(iter.nextId(), iter.nextOffset());
                        } catch (Throwable e) {
                            observer.onError(e);
                        }
                    });
                    return;
                }

            }
        }
        observer.onError(new IdExpiredException(startId));
    }

    private void addReadListener(LogSegment<T> segment, long startOffset, long startId, int limit, LogObserver<T> observer) {
        if (closed) {
            observer.onError(new RuntimeException("already closed"));
            return;
        }
        if (segment != currentSegment) {
            read(startOffset, startId, limit, observer);
            return;
        }
        segment.getWriter().addListener(startId, segmentConfig.readTimeout().toMillis(),
                new LogWriter.ReadListener() {
                    @Override
                    public void onTimeout() {
                        observer.onComplete(startId, startOffset);
                    }

                    @Override
                    public void onFlush() {
                        read(startOffset, startId, limit, observer);
                    }
                });
    }

    @Override
    public String toString() {
        return "IndexLogger[" + name + ']';
    }

    private void segmentCleanup() {
        try {
            while (segments.size() > segmentConfig.segmentNum()) {
                LogSegment<T> segment = segments.removeLast();
                LOG.info("IndexLogger[{}] cleanup Segment[{}->{}]", name, segment.startId(),
                        segment.endId());
                segment.delete();
            }
        } catch (Exception e) {
            LOG.error("Unexpected error on segment cleanup", e);
        }
    }

    @Override
    public void register(String metricPrefix, MeterRegistry registry, String... tags) {
        String[] newTags = Arrays.copyOf(tags, tags.length + 2);
        newTags[tags.length - 2] = "log_name";
        newTags[tags.length - 1] = name;
        for (LogSegment<T> segment : segments) {
            if (segment.getReader() instanceof HasMetrics hasMetrics) {
                readerExecutor.execute(() -> {
                    hasMetrics.register(metricPrefix, registry, newTags.clone());
                });
            }
            LogSegment.Writer<T> writer = segment.getWriter();
            if (writer instanceof HasMetrics hasMetrics) {
                executor.execute(() -> {
                    if (segment.getWriter() != null) {
                        hasMetrics.register(metricPrefix, registry, newTags);
                    }
                });
            }
        }
    }

    @Override
    public void close() {
        closed = true;
        executor.execute(() -> {
            currentSegment.getWriter().flush();
        });
        closeLockResources();
    }

    @Override
    public void expire(long idBefore) {
        executor.execute(() -> {
            Iterator<LogSegment<T>> iter = segments.descendingIterator();
            while (iter.hasNext()) {
                LogSegment<T> segment = iter.next();
                if (segment == currentSegment || segment.endId() >= idBefore) {
                    break;
                }
                LOG.info("IndexLogger[{}] expire Segment[{}->{}]", name, segment.startId(),
                        segment.endId());
                iter.remove();
                segment.delete();
            }
        });
    }

    private static class CallbackAdaptor implements Callback {
        private final FlushListener listener;
        private final boolean immediateCallback;
        private IdAndOffset idAndOffset;

        private CallbackAdaptor(FlushListener listener, boolean immediateCallback) {
            this.listener = listener;
            this.immediateCallback = immediateCallback;
        }

        @Override
        public void onAppend(long id, long offset) {
            if (immediateCallback) {
                listener.onReceive(new IdAndOffset(id, offset));
            } else {
                idAndOffset = new IdAndOffset(id, offset);
            }
        }

        @Override
        public void onPersist() {
            if (!immediateCallback) {
                listener.onReceive(idAndOffset);
            }
        }

        @Override
        public void onError(Throwable e) {
            listener.onError(e);
        }
    }
}
