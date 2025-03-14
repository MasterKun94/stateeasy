package io.masterkun.stateeasy.indexlogging.impl;

import io.masterkun.stateeasy.concurrent.EventExecutor;
import io.masterkun.stateeasy.indexlogging.HasMetrics;
import io.masterkun.stateeasy.indexlogging.LogConfig;
import io.masterkun.stateeasy.indexlogging.LogIterator;
import io.masterkun.stateeasy.indexlogging.Serializer;
import io.masterkun.stateeasy.indexlogging.exception.LogFullException;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LogSegmentImpl<T> implements LogSegment<T> {
    private static final Logger LOG = LoggerFactory.getLogger(LogSegmentImpl.class);
    private final Serializer<T> serializer;
    private final MetaInfo metaInfo;
    private final LogIndexer indexer;
    private final LogReader reader;
    private final LogConfig config;
    private final long initId;
    private LogWriter writer;

    LogSegmentImpl(MetaInfo metaInfo, LogIndexer indexer, LogReader reader, LogWriter writer,
                   Serializer<T> serializer, LogConfig config, long initId) {
        this.metaInfo = metaInfo;
        this.indexer = indexer;
        this.reader = reader;
        this.writer = writer;
        this.serializer = serializer;
        this.config = config;
        this.initId = initId;
    }

    public static <T> LogSegmentImpl<T> create(LogConfig config, long initId, long initOffset,
                                               EventExecutor executor,
                                               Serializer<T> serializer) throws IOException {
        if (!config.logDir().exists()) {
            config.logDir().mkdir();
        }
        var metaInfo = MetaInfo.create(Utils.metaFile(config, initId), initId, initOffset,
                config.indexSizeMax(), config.segmentSizeMax());
        var indexer = LogIndexer.create(Utils.indexFile(config, initId), config.indexSizeMax(),
                config.indexChunkSize(), config.indexPersistSize(), config.indexPersistInterval(),
                executor);
        var reader = LogReader.create(Utils.logFile(config, initId), config.segmentSizeMax());
        var writer = LogWriter.create(reader, indexer, executor, config.autoFlushSize(),
                config.autoFlushInterval(), config.serializeBufferDirect(),
                config.serializeBufferInit(), config.serializeBufferMax());
        LOG.info("LogSegment[{}-{}] is created", config.name(), initId);
        return new LogSegmentImpl<>(metaInfo, indexer, reader, writer, serializer, config, initId);
    }

    public static <T> LogSegmentImpl<T> recover(LogConfig config, long initId,
                                                EventExecutor executor, Serializer<T> serializer,
                                                boolean readOnly) throws IOException {
        var metaInfo = MetaInfo.recover(Utils.metaFile(config, initId));
        var indexer = LogIndexer.recover(Utils.indexFile(config, initId), metaInfo.idxLimit(),
                config.indexChunkSize(), config.indexPersistSize(), config.indexPersistInterval(),
                executor, readOnly);
        var reader = LogReader.recover(Utils.logFile(config, initId), indexer,
                metaInfo.logLimit(), readOnly);
        var writer = readOnly ? null :
                LogWriter.create(reader, indexer, executor, config.autoFlushSize(),
                        config.autoFlushInterval(), config.serializeBufferDirect(),
                        config.serializeBufferInit(), config.serializeBufferMax());
        LOG.info("LogSegment[{}-{}] is recovered, readOnly={}", config.name(), initId, readOnly);
        return new LogSegmentImpl<>(metaInfo, indexer, reader, writer, serializer, config, initId);
    }

    @Override
    public long startId() {
        return metaInfo.initId();
    }

    @Override
    public long endId() {
        return indexer.endId() == -1 ? -1 : realId(indexer.endId());
    }

    @Override
    public long nextId() {
        return realId(indexer.endId() == -1 ? 0 : indexer.endId() + 1);
    }

    @Override
    public long startOffset() {
        return metaInfo.initOffset();
    }

    @Override
    public long endOffset() {
        return indexer.endOffset() == -1 ? -1 : realOffset(indexer.endOffset());
    }

    @Override
    public long nextOffset() {
        return realOffset(indexer.endOffset() == -1 ? 0 :
                reader.getNextOffset(indexer.endOffset(), indexer.endId()));
    }

    @Override
    public Writer<T> getWriter() {
        if (writer == null) {
            throw new IllegalArgumentException("read only");
        }
        return new WriterImpl();
    }

    @Override
    public Reader<T> getReader() {
        return new ReaderImpl();
    }

    @Override
    public void setReadOnly() throws IOException {
        if (writer != null) {
            writer.flush();
            writer.close();
            metaInfo.setReadOnly();
            writer = null;
            LOG.info("LogSegment[{}-{}] switched to read only", config.name(), initId);
        }
    }

    @Override
    public boolean isReadOnly() {
        return writer == null;
    }

    @Override
    public void delete() {
        Utils.metaFile(config, initId).delete();
        Utils.logFile(config, initId).delete();
        Utils.indexFile(config, initId).delete();
        LOG.info("LogSegment[{}-{}] is deleted", config.name(), initId);
    }

    private long realId(int innerId) {
        return metaInfo.initId() + innerId;
    }

    private int innerId(long realId) {
        return (int) (realId - metaInfo.initId());
    }

    private long realOffset(int innerOffset) {
        return metaInfo.initOffset() + innerOffset;
    }

    private int innerOffset(long realOffset) {
        return (int) (realOffset - metaInfo.initOffset());
    }

    @Override
    public String toString() {
        return "LogSegmentImpl[" + config.name() + "-" + initId + ']';
    }

    private final class WriterImpl implements Writer<T>, HasMetrics {

        @Override
        public boolean put(T value, Callback callback, boolean flush) {
            try (Serializer.DataOut out = writer.open(flush, new LogWriter.WriteListener() {
                @Override
                public void onAppend(int id, int offset) {
                    callback.onAppend(realId(id), realOffset(offset));
                }

                @Override
                public void onFlush() {
                    callback.onPersist();
                }
            })) {
                serializer.serialize(value, out);
            } catch (LogFullException e) {
                return false;
            } catch (Throwable e) {
                callback.onError(e);
            }
            return true;
        }

        @Override
        public void flush() {
            writer.flush();
        }

        @Override
        public void addListener(long startFromId, long timeoutMills,
                                LogWriter.ReadListener listener) {
            writer.addListener(innerId(startFromId), timeoutMills, listener);
        }

        @Override
        public void register(String metricPrefix, MeterRegistry registry, String... tags) {
            if (writer instanceof HasMetrics hasMetrics) {
                hasMetrics.register(metricPrefix, registry, tags);
            }
        }
    }

    private final class ReaderImpl implements Reader<T>, HasMetrics {

        @Override
        public LogIterator<T> get(long offsetAfter, long id, int limit) {
            int innerId = innerId(id);
            int innerOffset = offsetAfter < startOffset() ?
                    indexer.offsetBefore(innerId) :
                    innerOffset(offsetAfter);
            return get(innerOffset, innerId, limit);
        }

        private LogIterator<T> get(int offsetAfter, int id, int limit) {
            if (offsetAfter < 0) {
                if (indexer.isEmpty()) {
                    return new LogIterator<>(initId, metaInfo.initOffset(),
                            LogSegmentIterator.empty(0, 0));
                }
                offsetAfter = indexer.offsetBefore(id);
            }
            LogSegmentIterator<T> iter = reader.get(offsetAfter, id, limit, serializer);
            return new LogIterator<>(initId, metaInfo.initOffset(), iter);
        }

        @Override
        public void register(String metricPrefix, MeterRegistry registry, String... tags) {
            if (indexer instanceof HasMetrics hasMetrics) {
                hasMetrics.register(metricPrefix, registry, tags);
            }
            if (reader instanceof HasMetrics hasMetrics) {
                hasMetrics.register(metricPrefix, registry, tags);
            }
        }
    }
}
