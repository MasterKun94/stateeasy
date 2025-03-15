package io.masterkun.stateeasy.core.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.Optional;
import io.masterkun.stateeasy.indexlogging.LogConfig;

import java.io.File;
import java.time.Duration;

public final class LogFileEventStoreConfig {
    @Optional
    private int threadNumPerDisk = 1;
    @Optional
    private String logDir = "store";
    @Optional
    private int segmentNum = 8;
    @Optional
    private ConfigMemorySize segmentSize = ConfigMemorySize.ofBytes(32 * 1024 * 1024);
    @Optional
    private ConfigMemorySize indexChunkSize = ConfigMemorySize.ofBytes(64 * 1024);
    @Optional
    private ConfigMemorySize indexPersistSize = ConfigMemorySize.ofBytes(128 * 1024);
    @Optional
    private Duration indexPersistInterval = Duration.ofMillis(10);
    @Optional
    private ConfigMemorySize autoFlushSize = ConfigMemorySize.ofBytes(8 * 1024);
    @Optional
    private Duration autoFlushInterval = Duration.ofMillis(10);
    @Optional
    private boolean serializeBufferDirect = true;
    @Optional
    private ConfigMemorySize serializeBufferInit = ConfigMemorySize.ofBytes(4 * 1024);
    @Optional
    private ConfigMemorySize serializeBufferMax = ConfigMemorySize.ofBytes(1024 * 1024);
    @Optional
    private Duration readTimeout = Duration.ofSeconds(1);

    public LogFileEventStoreConfig() {
    }

    public static LogFileEventStoreConfig create(Config config) {
        try {
            Class.forName(LogFileEventStoreConfig.class.getName());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return ConfigBeanFactory.create(config, LogFileEventStoreConfig.class);
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public ConfigMemorySize getSerializeBufferMax() {
        return serializeBufferMax;
    }

    public void setSerializeBufferMax(ConfigMemorySize serializeBufferMax) {
        this.serializeBufferMax = serializeBufferMax;
    }

    public ConfigMemorySize getSerializeBufferInit() {
        return serializeBufferInit;
    }

    public void setSerializeBufferInit(ConfigMemorySize serializeBufferInit) {
        this.serializeBufferInit = serializeBufferInit;
    }

    public boolean isSerializeBufferDirect() {
        return serializeBufferDirect;
    }

    public void setSerializeBufferDirect(boolean serializeBufferDirect) {
        this.serializeBufferDirect = serializeBufferDirect;
    }

    public Duration getAutoFlushInterval() {
        return autoFlushInterval;
    }

    public void setAutoFlushInterval(Duration autoFlushInterval) {
        this.autoFlushInterval = autoFlushInterval;
    }

    public ConfigMemorySize getAutoFlushSize() {
        return autoFlushSize;
    }

    public void setAutoFlushSize(ConfigMemorySize autoFlushSize) {
        this.autoFlushSize = autoFlushSize;
    }

    public Duration getIndexPersistInterval() {
        return indexPersistInterval;
    }

    public void setIndexPersistInterval(Duration indexPersistInterval) {
        this.indexPersistInterval = indexPersistInterval;
    }

    public ConfigMemorySize getIndexPersistSize() {
        return indexPersistSize;
    }

    public void setIndexPersistSize(ConfigMemorySize indexPersistSize) {
        this.indexPersistSize = indexPersistSize;
    }

    public ConfigMemorySize getIndexChunkSize() {
        return indexChunkSize;
    }

    public void setIndexChunkSize(ConfigMemorySize indexChunkSize) {
        this.indexChunkSize = indexChunkSize;
    }

    public ConfigMemorySize getSegmentSize() {
        return segmentSize;
    }

    public void setSegmentSize(ConfigMemorySize segmentSize) {
        this.segmentSize = segmentSize;
    }

    public int getSegmentNum() {
        return segmentNum;
    }

    public void setSegmentNum(int segmentNum) {
        this.segmentNum = segmentNum;
    }

    public String getLogDir() {
        return logDir;
    }

    public void setLogDir(String logDir) {
        this.logDir = logDir;
    }

    public int getThreadNumPerDisk() {
        return threadNumPerDisk;
    }

    public void setThreadNumPerDisk(int threadNumPerDisk) {
        this.threadNumPerDisk = threadNumPerDisk;
    }

    public LogConfig toLogConfig(String name) {
        return new LogConfig(
                name,
                new File(logDir),
                segmentNum,
                toMemorySizeInt(segmentSize),
                toMemorySizeInt(indexChunkSize),
                toMemorySizeInt(indexPersistSize),
                indexPersistInterval,
                toMemorySizeInt(autoFlushSize),
                autoFlushInterval,
                serializeBufferDirect,
                toMemorySizeInt(serializeBufferInit),
                toMemorySizeInt(serializeBufferMax),
                readTimeout
        );
    }

    private int toMemorySizeInt(ConfigMemorySize size) {
        long bytes = size.toBytes();
        if (bytes <= Integer.MAX_VALUE) {
            return (int) bytes;
        }
        throw new IllegalArgumentException("memory size to big");
    }
}
