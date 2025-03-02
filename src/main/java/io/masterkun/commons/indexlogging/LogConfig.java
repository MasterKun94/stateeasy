package io.masterkun.commons.indexlogging;

import java.io.File;
import java.time.Duration;

/**
 * A record representing the configuration for a logging system. This configuration includes various parameters
 * that control how logs are managed, such as the number of segments per log, segment size, and buffer settings.
 * <p>
 * The configuration can be created using the provided builder pattern, which allows for flexible and fluent
 * construction of the {@code LogConfig} instance.
 */
public record LogConfig(
        String name,
        File logDir,
        int segmentNumPerLog,
        int segmentSizeMax,
        int indexChunkSize,
        int indexPersistSize,
        Duration indexPersistInterval,
        int autoFlushSize,
        Duration autoFlushInterval,
        boolean serializeBufferDirect,
        int serializeBufferInit,
        int serializeBufferMax,
        Duration readTimeout) {

    public static Builder builder(String name, File logDir) {
        return new Builder(logDir, name);
    }

    public int indexSizeMax() {
        return segmentSizeMax / indexChunkSize * 8;
    }

    public Builder toBuilder() {
        return new Builder(logDir, name)
                .segmentNumPerLog(segmentNumPerLog)
                .segmentSizeMax(segmentSizeMax)
                .indexChunkSize(indexChunkSize)
                .indexPersistSize(indexPersistSize)
                .indexPersistInterval(indexPersistInterval)
                .autoFlushSize(autoFlushSize)
                .autoFlushInterval(autoFlushInterval)
                .serializeBufferDirect(serializeBufferDirect)
                .serializeBufferInit(serializeBufferInit)
                .serializeBufferMax(serializeBufferMax);
    }

    public static class Builder {
        private final String name;
        private File logDir;
        private int segmentNumPerLog = 8;
        private int segmentSizeMax = 16 * 1024 * 1024;
        private int indexChunkSize = 8192;
        private int indexPersistSize = 65535;
        private Duration indexPersistInterval = Duration.ofMillis(10);
        private int autoFlushSize = 8192;
        private Duration autoFlushInterval = Duration.ofMillis(10);
        private boolean serializeBufferDirect = true;
        private int serializeBufferInit = 4096;
        private int serializeBufferMax = 1024 * 1024;
        private Duration readTimeout = Duration.ofSeconds(10);

        public Builder(File logDir, String name) {
            this.logDir = logDir;
            this.name = name;
        }

        public Builder logDir(File logDir) {
            this.logDir = logDir;
            return this;
        }

        public Builder segmentNumPerLog(int segmentNumPerLog) {
            this.segmentNumPerLog = segmentNumPerLog;
            return this;
        }

        public Builder segmentSizeMax(int logSizeMax) {
            this.segmentSizeMax = logSizeMax;
            return this;
        }

        public Builder indexChunkSize(int indexChunkSize) {
            this.indexChunkSize = indexChunkSize;
            return this;
        }

        public Builder indexPersistSize(int indexPersistSize) {
            this.indexPersistSize = indexPersistSize;
            return this;
        }

        public Builder indexPersistInterval(Duration indexPersistInterval) {
            this.indexPersistInterval = indexPersistInterval;
            return this;
        }

        public Builder autoFlushSize(int logAutoFlushSize) {
            this.autoFlushSize = logAutoFlushSize;
            return this;
        }

        public Builder autoFlushInterval(Duration logAutoFlushInterval) {
            this.autoFlushInterval = logAutoFlushInterval;
            return this;
        }

        public Builder serializeBufferDirect(boolean serializeBufferDirect) {
            this.serializeBufferDirect = serializeBufferDirect;
            return this;
        }

        public Builder serializeBufferInit(int serializeBufferInit) {
            this.serializeBufferInit = serializeBufferInit;
            return this;
        }

        public Builder serializeBufferMax(int serializeBufferMax) {
            this.serializeBufferMax = serializeBufferMax;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public LogConfig build() {
            return new LogConfig(
                    name,
                    logDir,
                    segmentNumPerLog,
                    segmentSizeMax,
                    indexChunkSize,
                    indexPersistSize,
                    indexPersistInterval,
                    autoFlushSize,
                    autoFlushInterval,
                    serializeBufferDirect,
                    serializeBufferInit,
                    serializeBufferMax,
                    readTimeout
            );
        }
    }
}
