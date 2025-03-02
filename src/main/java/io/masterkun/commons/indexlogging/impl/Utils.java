package io.masterkun.commons.indexlogging.impl;

import io.masterkun.commons.indexlogging.LogConfig;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.OpenOption;
import java.nio.file.StandardOpenOption;

public class Utils {
    public static int crc(int id, int len) {
        return (31 + id) * 17 + len;
    }

    public static MappedByteBuffer create(File file, int sizeLimit) throws IOException {
        if (file.exists()) {
            throw new FileAlreadyExistsException(file.toString());
        }
        try (FileChannel ch = FileChannel.open(file.toPath(),
                StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE)) {
            return ch.map(FileChannel.MapMode.READ_WRITE, 0, sizeLimit);
        }
    }

    public static MappedByteBuffer createExistence(File file, int sizeLimit, boolean readOnly) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }
        MappedByteBuffer buffer;
        try (FileChannel ch = FileChannel.open(file.toPath(), readOnly ?
                new OpenOption[]{StandardOpenOption.READ} :
                new OpenOption[]{StandardOpenOption.READ, StandardOpenOption.WRITE})) {
            FileChannel.MapMode mode = readOnly ?
                    FileChannel.MapMode.READ_ONLY : FileChannel.MapMode.READ_WRITE;
            buffer = ch.map(mode, 0, sizeLimit);
        }
        return buffer;
    }


    public static File metaFile(LogConfig config, long initId) {
        return new File(config.logDir(), "segment-" + config.name() + "-" + initId + ".meta");
    }

    public static File indexFile(LogConfig config, long initId) {
        return new File(config.logDir(), "segment-" + config.name() + "-" + initId + ".idx");
    }

    public static File logFile(LogConfig config, long initId) {
        return new File(config.logDir(), "segment-" + config.name() + "-" + initId + ".data");
    }

    public static long extractInitId(LogConfig config, File file) {
        String prefix = "segment-" + config.name() + "-";
        String suffix = ".meta";
        String fileName = file.getName();
        if (!fileName.startsWith(prefix)) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        if (!fileName.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid file name: " + fileName);
        }
        return Long.parseLong(fileName.substring(prefix.length(), fileName.length() - suffix.length()));
    }

    public static String metricName(String prefix, String name) {
        if (prefix == null || prefix.isEmpty()) {
            return name;
        }
        if (prefix.endsWith(".")) {
            return prefix + name;
        }
        return prefix + "." + name;
    }
}
