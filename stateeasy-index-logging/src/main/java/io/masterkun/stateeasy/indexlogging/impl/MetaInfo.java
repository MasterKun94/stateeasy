package io.masterkun.stateeasy.indexlogging.impl;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.FileAlreadyExistsException;
import java.util.Objects;
import java.util.Properties;

public class MetaInfo {
    private final File file;
    private final long initId;
    private final long initOff;
    private final int idxLimit;
    private final int logLimit;
    private boolean readOnly;

    private MetaInfo(File file, long initId, long initOff, int idxLimit, int logLimit,
                     boolean readOnly) {
        this.file = file;
        this.initId = initId;
        this.initOff = initOff;
        this.idxLimit = idxLimit;
        this.logLimit = logLimit;
        this.readOnly = readOnly;
    }

    public static MetaInfo create(File file, long initId, long initOff, int idxLimit,
                                  int logLimit) throws IOException {
        if (file.exists()) {
            throw new FileAlreadyExistsException(file.toString());
        }
        Properties prop = new Properties();
        prop.put(MetaKeys.INIT_ID.getKey(), String.valueOf(initId));
        prop.put(MetaKeys.INIT_OFF.getKey(), String.valueOf(initOff));
        prop.put(MetaKeys.IDX_LMT.getKey(), String.valueOf(idxLimit));
        prop.put(MetaKeys.LOG_LEN.getKey(), String.valueOf(logLimit));
        try (var out = new FileOutputStream(file, false)) {
            prop.store(out, null);
            out.flush();
        }
        return new MetaInfo(file, initId, initOff, idxLimit, logLimit, false);
    }

    public static MetaInfo recover(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException(file.toString());
        }

        Properties prop = new Properties();
        try (var in = new FileInputStream(file.toString())) {
            prop.load(in);
        }
        long initId = Long.parseLong(getRequired(prop, MetaKeys.INIT_ID));
        long initOff = Long.parseLong(getRequired(prop, MetaKeys.INIT_OFF));
        int idxLimit = Integer.parseInt(getRequired(prop, MetaKeys.IDX_LMT));
        int logLimit = Integer.parseInt(getRequired(prop, MetaKeys.LOG_LEN));
        boolean readOnly = Boolean.parseBoolean(get(prop, MetaKeys.READ_ONLY));
        return new MetaInfo(file, initId, initOff, idxLimit, logLimit, readOnly);
    }

    private static String getRequired(Properties prop, MetaKeys key) {
        return Objects.requireNonNull(get(prop, key),
                "MetaValue[" + key.getKey() + "]");
    }

    private static String get(Properties prop, MetaKeys key) {
        return prop.getProperty(key.getKey());
    }

    public long initId() {
        return initId;
    }

    public long initOffset() {
        return initOff;
    }

    public int idxLimit() {
        return idxLimit;
    }

    public int logLimit() {
        return logLimit;
    }

    public boolean readOnly() {
        return readOnly;
    }

    public void setReadOnly() throws IOException {
        if (readOnly) {
            return;
        }
        try (var out = new FileOutputStream(file, true)) {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out));
            writer.newLine();
            writer.write(MetaKeys.READ_ONLY.getKey() + "=true");
            writer.flush();
        } finally {
            readOnly = true;
        }
    }
}
