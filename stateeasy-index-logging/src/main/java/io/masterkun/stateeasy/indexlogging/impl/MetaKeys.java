package io.masterkun.stateeasy.indexlogging.impl;

public enum MetaKeys {
    INIT_ID("init_id"),
    INIT_OFF("init_off"),
    IDX_LMT("idx_lmt"),
    LOG_LEN("log_lmt"),
    READ_ONLY("ro");

    private final String key;

    MetaKeys(String key) {
        this.key = key;
    }

    public static MetaKeys of(String key) {
        for (MetaKeys value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        return null;
    }

    public String getKey() {
        return key;
    }
}
