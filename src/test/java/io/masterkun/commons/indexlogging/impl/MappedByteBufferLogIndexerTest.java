package io.masterkun.commons.indexlogging.impl;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.MappedByteBuffer;

public class MappedByteBufferLogIndexerTest {

    private static final File testFile = new File("src/test/resources/.test/index.log");

    @BeforeClass
    public static void beforeClass() throws IOException {
        if (testFile.exists()) {
            testFile.delete();
        }
        testFile.getParentFile().mkdirs();
    }

    @AfterClass
    public static void afterClass() throws IOException {
        testFile.delete();
    }

    @Test
    public void test() throws IOException {
        MappedByteBufferLogIndexer indexer = MappedByteBufferLogIndexer.create(testFile, 1024 * 1024);
        MappedByteBuffer buffer = indexer.getBuffer();
        System.out.println(testFile.length());
        Assert.assertEquals(-1, indexer.endId());
        Assert.assertEquals(-1, indexer.endOffset());

        indexer.update(2, 123);
        Assert.assertEquals(2, indexer.endId());
        Assert.assertEquals(123, indexer.endOffset());
        Assert.assertEquals(8, buffer.position());

        indexer.update(5, 456);
        Assert.assertEquals(5, indexer.endId());
        Assert.assertEquals(456, indexer.endOffset());
        Assert.assertEquals(16, buffer.position());

        indexer.update(8, 789);
        Assert.assertEquals(8, indexer.endId());
        Assert.assertEquals(789, indexer.endOffset());
        Assert.assertEquals(24, buffer.position());

        Assert.assertEquals(0, indexer.offsetBefore(0));
        Assert.assertEquals(0, indexer.offsetBefore(1));
        Assert.assertEquals(123, indexer.offsetBefore(2));
        Assert.assertEquals(123, indexer.offsetBefore(3));
        Assert.assertEquals(456, indexer.offsetBefore(5));
        Assert.assertEquals(456, indexer.offsetBefore(6));
        Assert.assertEquals(789, indexer.offsetBefore(8));
        Assert.assertEquals(789, indexer.offsetBefore(9));
        indexer.persist();

        indexer.update(9, 800);
        indexer.update(10, 811);
        indexer.update(11, 822);
        indexer.update(12, 833);
        indexer.update(13, 854);
        indexer.update(100, 900);
        indexer.update(101, 911);
        indexer.update(102, 922);
        Assert.assertEquals(0, indexer.offsetBefore(0));
        Assert.assertEquals(0, indexer.offsetBefore(1));
        Assert.assertEquals(123, indexer.offsetBefore(2));
        Assert.assertEquals(123, indexer.offsetBefore(3));
        Assert.assertEquals(456, indexer.offsetBefore(5));
        Assert.assertEquals(456, indexer.offsetBefore(6));
        Assert.assertEquals(789, indexer.offsetBefore(8));
        Assert.assertEquals(800, indexer.offsetBefore(9));
        Assert.assertEquals(811, indexer.offsetBefore(10));
        Assert.assertEquals(822, indexer.offsetBefore(11));
        Assert.assertEquals(833, indexer.offsetBefore(12));
        Assert.assertEquals(854, indexer.offsetBefore(13));
        Assert.assertEquals(854, indexer.offsetBefore(14));
        Assert.assertEquals(854, indexer.offsetBefore(99));
        Assert.assertEquals(900, indexer.offsetBefore(100));
        Assert.assertEquals(911, indexer.offsetBefore(101));
        Assert.assertEquals(922, indexer.offsetBefore(922));
        Assert.assertEquals(922, indexer.offsetBefore(923));
        indexer.persist();

        MappedByteBufferLogIndexer recover = MappedByteBufferLogIndexer.recover(testFile, 1024 * 1024, false);
        Assert.assertEquals(102, recover.endId());
        Assert.assertEquals(922, recover.endOffset());
        Assert.assertEquals(buffer.position(), recover.getBuffer().position());
        Assert.assertEquals(0, indexer.offsetBefore(0));
        Assert.assertEquals(0, indexer.offsetBefore(1));
        Assert.assertEquals(123, recover.offsetBefore(2));
        Assert.assertEquals(922, recover.offsetBefore(923));

    }
}
