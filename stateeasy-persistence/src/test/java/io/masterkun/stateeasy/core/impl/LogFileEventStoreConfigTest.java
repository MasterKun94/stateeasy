package io.masterkun.stateeasy.core.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigMemorySize;
import org.junit.Test;

import java.time.Duration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LogFileEventStoreConfigTest {

    @Test
    public void testCreateWithDefaultConfig() {
        Config config = ConfigFactory.empty();
        LogFileEventStoreConfig logFileEventStoreConfig = LogFileEventStoreConfig.create(config);

        assertEquals(1, logFileEventStoreConfig.getThreadNumPerDisk());
        assertEquals("store", logFileEventStoreConfig.getLogDir());
        assertEquals(8, logFileEventStoreConfig.getSegmentNum());
        assertEquals(ConfigMemorySize.ofBytes(32 * 1024 * 1024),
                logFileEventStoreConfig.getSegmentSize());
        assertEquals(ConfigMemorySize.ofBytes(64 * 1024),
                logFileEventStoreConfig.getIndexChunkSize());
        assertEquals(ConfigMemorySize.ofBytes(128 * 1024),
                logFileEventStoreConfig.getIndexPersistSize());
        assertEquals(Duration.ofMillis(10), logFileEventStoreConfig.getIndexPersistInterval());
        assertEquals(ConfigMemorySize.ofBytes(8 * 1024),
                logFileEventStoreConfig.getAutoFlushSize());
        assertEquals(Duration.ofMillis(10), logFileEventStoreConfig.getAutoFlushInterval());
        assertTrue(logFileEventStoreConfig.isSerializeBufferDirect());
        assertEquals(ConfigMemorySize.ofBytes(4 * 1024),
                logFileEventStoreConfig.getSerializeBufferInit());
        assertEquals(ConfigMemorySize.ofBytes(1024 * 1024),
                logFileEventStoreConfig.getSerializeBufferLimit());
        assertEquals(Duration.ofSeconds(1), logFileEventStoreConfig.getReadTimeout());
    }

    @Test
    public void testCreateWithCustomConfig() {
        Config config = ConfigFactory.parseString(
                "thread-num-per-disk: 2\n" +
                        "log-dir: custom-store\n" +
                        "segment-num: 16\n" +
                        "segment-size: 64M\n" +
                        "index-chunk-size: 128K\n" +
                        "index-persist-size: 256K\n" +
                        "index-persist-interval: 20ms\n" +
                        "auto-flush-size: 16K\n" +
                        "auto-flush-interval: 20ms\n" +
                        "serialize-buffer-direct: false\n" +
                        "serialize-buffer-init: 8K\n" +
                        "serialize-buffer-limit: 2M\n" +
                        "read-timeout: 2s"
        );
        LogFileEventStoreConfig logFileEventStoreConfig = LogFileEventStoreConfig.create(config);

        assertEquals(2, logFileEventStoreConfig.getThreadNumPerDisk());
        assertEquals("custom-store", logFileEventStoreConfig.getLogDir());
        assertEquals(16, logFileEventStoreConfig.getSegmentNum());
        assertEquals(ConfigMemorySize.ofBytes(64 * 1024 * 1024),
                logFileEventStoreConfig.getSegmentSize());
        assertEquals(ConfigMemorySize.ofBytes(128 * 1024),
                logFileEventStoreConfig.getIndexChunkSize());
        assertEquals(ConfigMemorySize.ofBytes(256 * 1024),
                logFileEventStoreConfig.getIndexPersistSize());
        assertEquals(Duration.ofMillis(20), logFileEventStoreConfig.getIndexPersistInterval());
        assertEquals(ConfigMemorySize.ofBytes(16 * 1024),
                logFileEventStoreConfig.getAutoFlushSize());
        assertEquals(Duration.ofMillis(20), logFileEventStoreConfig.getAutoFlushInterval());
        assertFalse(logFileEventStoreConfig.isSerializeBufferDirect());
        assertEquals(ConfigMemorySize.ofBytes(8 * 1024),
                logFileEventStoreConfig.getSerializeBufferInit());
        assertEquals(ConfigMemorySize.ofBytes(2 * 1024 * 1024),
                logFileEventStoreConfig.getSerializeBufferLimit());
        assertEquals(Duration.ofSeconds(2), logFileEventStoreConfig.getReadTimeout());
    }
}
