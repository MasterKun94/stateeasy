package io.masterkun.commons.indexlogging;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.time.Duration;

public class MPMCRunnerTest {
    static File testDir = new File("src/test/resources/.test/MPMCRunnerTest");

    @BeforeClass
    public static void startup() {
        if (testDir.exists()) {
            cleanup();
        }
        testDir.mkdirs();
    }

    @AfterClass
    public static void cleanup() {
        File[] files = testDir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @Test
    public void test() throws Exception {
        LogSystem system = new LogSystem(2);
        LogConfig config = LogConfig.builder("test", testDir)
                .segmentSizeMax(1024 * 1024)
                .autoFlushInterval(Duration.ofMillis(2))
                .readTimeout(Duration.ofMillis(1000))
                .build();
        Serializer<String> serializer = new Serializer<>() {
            @Override
            public void serialize(String obj, DataOut out) throws IOException {
                out.writeUTF(obj);
            }

            @Override
            public String deserialize(DataIn in) throws IOException {
                return in.readUTF();
            }
        };
        EventLogger<String> logger = system.get(config, serializer);
        MPMCRunner runner = new MPMCRunner(logger, 3, 3, 20000);
        runner.run();

        LogSystem system2 = new LogSystem(2);
        Assert.assertThrows(OverlappingFileLockException.class, () -> system2.get(config, serializer));
        system.shutdown().join();
        EventLogger<String> logger2 = system2.get(config, serializer);
        MPMCRunner runner2 = new MPMCRunner(logger2, 3, 3, 20000);
        runner2.runConsumer();
        runner2.run();
        runner2.expire();
    }

}
