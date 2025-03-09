package io.masterkun.stateeasy.indexlogging.exception;

import java.io.File;
import java.io.IOException;

public class FileAlreadyLockedException extends IOException {
    public FileAlreadyLockedException(File file) {
        super("File " + file + " already been locked by other process");
    }
}
