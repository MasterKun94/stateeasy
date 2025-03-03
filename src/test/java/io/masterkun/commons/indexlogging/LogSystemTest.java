package io.masterkun.commons.indexlogging;


import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.OverlappingFileLockException;
import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class LogSystemTest {
    static File testDir = new File("src/test/resources/.test/LogSystemTest");

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
        // TODO
        LogSystem system = new LogSystem(2);
        LogConfig config = LogConfig.builder("test", testDir)
                .autoFlushInterval(Duration.ofMillis(100))
                .readTimeout(Duration.ofMillis(200))
                .build();

        EventLogger<String> logger = system.get(config, new StringSerializer());

        var observer = new TestObserver();
        logger.read(0, 10, observer);
        long start = System.currentTimeMillis();
        IdAndOffset nextIdAndOffset = observer.getFuture().join();
        long last = System.currentTimeMillis() - start;
        Assert.assertTrue("last: " + last, last > 180 && last < 220);
        Assert.assertEquals(0, nextIdAndOffset.id());

        CompletableFuture<IdAndOffset> future = logger.write("Hello");
        Assert.assertFalse(future.isDone());
        start = System.currentTimeMillis();
        Assert.assertThrows(TimeoutException.class, () -> future.get(10, TimeUnit.MILLISECONDS));
        IdAndOffset idAndOffset = future.join();
        last = System.currentTimeMillis() - start;
        Assert.assertTrue("last: " + last, last > 80 && last < 120);
        Assert.assertEquals(nextIdAndOffset, idAndOffset);
        observer = new TestObserver(
                new CheckRecord(0, 0, "Hello")
        );
        logger.read(0, 10, observer);
        nextIdAndOffset = observer.getFuture().join();
        Assert.assertEquals(1, nextIdAndOffset.id());

        var f2 = logger.write("World", true);
        start = System.currentTimeMillis();
        idAndOffset = f2.join();
        last = System.currentTimeMillis() - start;
        Assert.assertTrue("last: " + last, last < 30);
        Assert.assertEquals(nextIdAndOffset, idAndOffset);

        observer = new TestObserver(
                new CheckRecord(idAndOffset, "World")
        );
        logger.read(1, 10, observer);
        nextIdAndOffset = observer.getFuture().join();
        Assert.assertEquals(2, nextIdAndOffset.id());

        observer = new TestObserver(
                new CheckRecord(0, 0, "Hello")
        );
        logger.read(0, 1, observer);
        nextIdAndOffset = observer.getFuture().join();
        Assert.assertEquals(1, nextIdAndOffset.id());

        observer = new TestObserver(
                new CheckRecord(0, 0, "Hello"),
                new CheckRecord(idAndOffset, "World")
        );
        logger.read(0, 10, observer);
        nextIdAndOffset = observer.getFuture().join();
        Assert.assertEquals(2, nextIdAndOffset.id());

        LogSystem system2 = new LogSystem(2);
        Assert.assertThrows(OverlappingFileLockException.class, () -> system2.get(config, new StringSerializer()));
        system.shutdown().join();
        EventLogger<String> logger2 = system2.get(config, new StringSerializer());

        observer = new TestObserver(
                new CheckRecord(0, 0, "Hello"),
                new CheckRecord(idAndOffset, "World")
        );
        logger2.read(0, 10, observer);
        nextIdAndOffset = observer.getFuture().join();
        Assert.assertEquals(2, nextIdAndOffset.id());
    }

    private static class StringSerializer implements Serializer<String> {

        @Override
        public void serialize(String obj, DataOut out) throws IOException {
            out.writeUTF(obj);
        }

        @Override
        public String deserialize(DataIn in) throws IOException {
            return in.readUTF();
        }
    }

    private static class TestObserver implements LogObserver<String> {
        private final Queue<CheckRecord> elems = new LinkedList<>();
        private final CompletableFuture<IdAndOffset> future = new CompletableFuture<>();

        public TestObserver(CheckRecord... elems) {
            this.elems.addAll(Arrays.asList(elems));
        }

        @Override
        public void onNext(long id, long offset, String value) {
            CheckRecord poll = elems.poll();
            Assert.assertNotNull(poll);
            Assert.assertEquals(poll.id(), id);
            Assert.assertEquals(poll.offset(), offset);
            Assert.assertEquals(poll.value(), value);
        }

        @Override
        public void onComplete(long nextId, long nextOffset) {
            Assert.assertEquals(0, elems.size());
            future.complete(new IdAndOffset(nextId, nextOffset));
        }

        @Override
        public void onError(Throwable e) {
            future.completeExceptionally(e);
        }

        public CompletableFuture<IdAndOffset> getFuture() {
            return future;
        }
    }

    private record CheckRecord(long id, long offset, String value) {
        public CheckRecord(IdAndOffset idAndOffset, String value) {
            this(idAndOffset.id(), idAndOffset.offset(), value);
        }
    }

    ;
}
