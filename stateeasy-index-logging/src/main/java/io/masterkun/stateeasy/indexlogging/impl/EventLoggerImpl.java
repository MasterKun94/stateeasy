package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.concurrent.EventStageListener;
import io.masterkun.stateeasy.concurrent.HasMetrics;
import io.masterkun.stateeasy.indexlogging.EventLogger;
import io.masterkun.stateeasy.indexlogging.IdAndOffset;
import io.masterkun.stateeasy.indexlogging.LogConfig;
import io.masterkun.stateeasy.indexlogging.LogObserver;
import io.masterkun.stateeasy.indexlogging.Serializer;
import io.masterkun.stateeasy.indexlogging.exception.FileAlreadyLockedException;
import io.masterkun.stateeasy.indexlogging.exception.IdExpiredException;
import io.masterkun.stateeasy.indexlogging.exception.LogCorruptException;
import io.micrometer.core.instrument.MeterRegistry;
import org.jetbrains.annotations.Nullable;
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
import java.util.function.Consumer;

/**
 * An implementation of the {@link EventLogger} interface that manages a series of log segments for
 * storing and retrieving events. This class provides functionality to write, read, and manage log
 * segments, as well as register metrics and handle cleanup.
 * <p>
 * The log segments are managed in a deque, with new segments being added to the front. The current
 * segment is where new writes occur, and once it reaches its capacity, a new segment is created.
 * Segments can be read from, and old segments are periodically cleaned up based on the
 * configuration.
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
    private final EventExecutor executor;
    private final EventExecutor readerExecutor;
    private final FileLock dirLock;
    private final FileOutputStream lockStream;
    private LogSegment<T> currentSegment;
    private volatile boolean closed;

    public EventLoggerImpl(String name,
                           LogConfig config,
                           Serializer<T> serializer,
                           EventExecutor executor,
                           EventExecutor readerExecutor) throws IOException {
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
            File[] metaFiles = logDir.listFiles(file -> {
                String fileName = file.getName();
                return fileName.endsWith(".meta") &&
                        fileName.startsWith(Utils.namePrefix(config));
            });
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

    private boolean checkClose(Consumer<Throwable> listener) {
        if (closed) {
            listener.accept(new RuntimeException("already closed"));
            return true;
        } else {
            return false;
        }
    }

    public String name() {
        return name;
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
    public void write(T obj, boolean flush, boolean immediateCallback,
                      EventStageListener<IdAndOffset> listener) {
        if (checkClose(listener::failure)) {
            return;
        }
        if (executor.inExecutor()) {
            doWrite(obj, flush, immediateCallback, listener);
        } else {
            executor.execute(() -> doWrite(obj, flush, immediateCallback, listener));
        }
    }

    private void doWrite(T obj, boolean flush, boolean immediateCallback,
                         EventStageListener<IdAndOffset> listener) {
        assert executor.inExecutor();
        if (checkClose(listener::failure)) {
            return;
        }
        var callback = new CallbackAdaptor(listener, immediateCallback);
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
    }

    @Override
    public void flush(EventStageListener<Void> listener) {
        if (checkClose(listener::failure)) {
            return;
        }
        if (executor.inExecutor()) {
            doFlush(listener);
        } else {
            executor.execute(() -> doFlush(listener));
        }
    }

    private void doFlush(EventStageListener<Void> listener) {
        if (checkClose(listener::failure)) {
            return;
        }
        try {
            currentSegment.getWriter().flush();
            listener.success(null);
        } catch (Throwable e) {
            listener.failure(e);
        }
    }

    @Override
    public void read(long startId, int limit, LogObserver<T> observer) {
        read(0, startId, limit, observer);
    }

    @Override
    public void read(long startOffset, long startId, int limit, LogObserver<T> observer) {
        if (checkClose(observer::onError)) {
            return;
        }
        if (readerExecutor.inExecutor()) {
            for (LogSegment<T> segment : segments) {
                if (segment.startId() <= startId) {
                    if (segment == currentSegment) {
                        doReadCurrentSegment(segment, startOffset, startId, limit, observer);
                    } else {
                        doReadSegment(segment, startOffset, startId, limit, observer);
                    }
                    return;
                }
            }
        } else {
            for (LogSegment<T> segment : segments) {
                if (segment.startId() <= startId) {
                    if (segment == currentSegment) {
                        readerExecutor.execute(() -> doReadCurrentSegment(segment, startOffset,
                                startId, limit, observer));
                    } else {
                        readerExecutor.execute(() -> doReadSegment(segment, startOffset, startId,
                                limit, observer));
                    }
                    return;

                }
            }
        }
        observer.onError(new IdExpiredException(startId));
    }

    @Override
    public void readOne(long id, EventStageListener<T> listener) {
        readOne(0, id, listener);
    }

    @Override
    public void readOne(long startOffset, long id, EventStageListener<@Nullable T> listener) {
        read(startOffset, id, 1, new LogObserver<>() {
            T value;

            @Override
            public void onNext(long id, long offset, T value) {
                assert this.value == null;
                this.value = value;
            }

            @Override
            public void onComplete(long nextId, long nextOffset) {
                listener.success(value);
            }

            @Override
            public void onError(Throwable e) {
                listener.failure(e);
            }
        });
    }

    private void doReadCurrentSegment(LogSegment<T> segment, long startOffset, long startId,
                                      int limit, LogObserver<T> observer) {
        assert readerExecutor.inExecutor();
        if (checkClose(observer::onError)) {
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
                executor.execute(() -> addReadListener(segment, startOffset, startId, limit,
                        observer));
            }
        } catch (Throwable e) {
            observer.onError(e);
        }
    }

    private void doReadSegment(LogSegment<T> segment, long startOffset, long startId, int limit,
                               LogObserver<T> observer) {
        assert readerExecutor.inExecutor();
        if (checkClose(observer::onError)) {
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
    }

    private void addReadListener(LogSegment<T> segment, long startOffset, long startId, int limit
            , LogObserver<T> observer) {
        assert executor.inExecutor();
        if (checkClose(observer::onError)) {
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
    public void expire(long idBefore, EventStageListener<Boolean> listener) {
        executor.execute(() -> {
            try {
                Iterator<LogSegment<T>> iter = segments.descendingIterator();
                boolean removed = false;
                while (iter.hasNext()) {
                    LogSegment<T> segment = iter.next();
                    if (segment == currentSegment || segment.endId() >= idBefore) {
                        break;
                    }
                    LOG.info("IndexLogger[{}] expire Segment[{}->{}]", name, segment.startId(),
                            segment.endId());
                    iter.remove();
                    segment.delete();
                    removed = true;
                }
                listener.success(removed);
            } catch (Throwable e) {
                listener.failure(e);
            }
        });
    }

    @Override
    public EventExecutor executor() {
        return executor;
    }

    private static class CallbackAdaptor implements Callback {
        private final EventStageListener<IdAndOffset> listener;
        private final boolean immediateCallback;
        private IdAndOffset idAndOffset;

        private CallbackAdaptor(EventStageListener<IdAndOffset> listener,
                                boolean immediateCallback) {
            this.listener = listener;
            this.immediateCallback = immediateCallback;
        }

        @Override
        public void onAppend(long id, long offset) {
            if (immediateCallback) {
                listener.success(new IdAndOffset(id, offset));
            } else {
                idAndOffset = new IdAndOffset(id, offset);
            }
        }

        @Override
        public void onPersist() {
            if (!immediateCallback) {
                listener.success(idAndOffset);
            }
        }

        @Override
        public void onError(Throwable e) {
            listener.failure(e);
        }
    }
}
