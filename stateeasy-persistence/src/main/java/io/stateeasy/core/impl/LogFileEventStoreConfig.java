package io.stateeasy.core.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigBeanFactory;
import com.typesafe.config.ConfigMemorySize;
import com.typesafe.config.Optional;
import io.stateeasy.indexlogging.LogConfig;

import java.io.File;
import java.time.Duration;

public final class LogFileEventStoreConfig {
    /**
     * Specifies the number of threads to be used per disk for I/O operations. This configuration
     * can help in optimizing the performance by controlling the level of parallelism for disk I/O,
     * which is particularly useful in high-load scenarios or when dealing with multiple disks.
     */
    @Optional
    private int threadNumPerDisk = 1;
    /**
     * The directory where log files are stored. This is an optional configuration, and if not set,
     * it defaults to "store".
     */
    @Optional
    private String logDir = "store";
    /**
     * Represents the number of segments to be used in the log file event store. This value can be
     * adjusted to optimize performance and resource usage based on the specific requirements of the
     * application.
     */
    @Optional
    private int segmentNum = 8;
    /**
     * Represents the size of each segment in the log file. This configuration influences how data
     * is divided and stored, affecting performance and resource usage.
     */
    @Optional
    private ConfigMemorySize segmentSize = ConfigMemorySize.ofBytes(32 * 1024 * 1024);
    /**
     * Specifies the size of each index chunk in the event store. This configuration is used to
     * control how data is segmented and indexed, which can impact both performance and resource
     * usage. The default value is 64 kilobytes.
     */
    @Optional
    private ConfigMemorySize indexChunkSize = ConfigMemorySize.ofBytes(64 * 1024);
    /**
     * Specifies the size in bytes at which the index will be persisted to disk. This setting helps
     * in controlling how frequently the in-memory index is flushed to the persistent storage,
     * balancing between performance and data safety.
     */
    @Optional
    private ConfigMemorySize indexPersistSize = ConfigMemorySize.ofBytes(128 * 1024);
    /**
     * Defines the interval at which index persistence should be triggered. This configuration
     * option is optional, and if not set, it defaults to 10 milliseconds. The value is used to
     * control how often the in-memory index is persisted to disk, helping to balance between
     * performance and data durability.
     */
    @Optional
    private Duration indexPersistInterval = Duration.ofMillis(10);
    /**
     * The size in bytes at which the log buffer will be automatically flushed to disk. This setting
     * helps to balance between memory usage and I/O operations, as smaller sizes will cause more
     * frequent writes, while larger sizes may use more memory.
     */
    @Optional
    private ConfigMemorySize autoFlushSize = ConfigMemorySize.ofBytes(8 * 1024);
    /**
     * The interval at which the log file is automatically flushed to disk. This configuration
     * determines how often the system will attempt to flush the in-memory data to the persistent
     * storage, ensuring that the data is not lost in case of a failure. A shorter interval provides
     * better durability but may impact performance due to more frequent I/O operations.
     */
    @Optional
    private Duration autoFlushInterval = Duration.ofMillis(10);
    /**
     * Indicates whether the buffer used for serialization should be direct or not. Setting this to
     * true will use a direct buffer, while false will use a non-direct (heap) buffer.
     */
    @Optional
    private boolean serializeBufferDirect = true;
    /**
     * The initial size of the buffer used for serializing events. This configuration sets the
     * starting point for the buffer, which can grow or shrink based on the actual needs during the
     * serialization process.
     */
    @Optional
    private ConfigMemorySize serializeBufferInit = ConfigMemorySize.ofBytes(4 * 1024);
    /**
     * Defines the maximum size of the buffer used for object serialization. This setting helps in
     * controlling the memory usage and performance during the serialization process. The default
     * value is 1 MB (1024 * 1024 bytes).
     *
     * @see #getSerializeBufferLimit()
     * @see #setSerializeBufferLimit(ConfigMemorySize)
     */
    @Optional
    private ConfigMemorySize serializeBufferLimit = ConfigMemorySize.ofBytes(1024 * 1024);
    /**
     * The read timeout duration for operations that involve reading from the event store. This
     * value defines the maximum amount of time to wait for a read operation to complete before it
     * is considered to have timed out. The default value is 1 second.
     */
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

    public ConfigMemorySize getSerializeBufferLimit() {
        return serializeBufferLimit;
    }

    public void setSerializeBufferLimit(ConfigMemorySize serializeBufferLimit) {
        this.serializeBufferLimit = serializeBufferLimit;
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
                new File(logDir).getAbsoluteFile(),
                segmentNum,
                toMemorySizeInt(segmentSize),
                toMemorySizeInt(indexChunkSize),
                toMemorySizeInt(indexPersistSize),
                indexPersistInterval,
                toMemorySizeInt(autoFlushSize),
                autoFlushInterval,
                serializeBufferDirect,
                toMemorySizeInt(serializeBufferInit),
                toMemorySizeInt(serializeBufferLimit),
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
