package com.rankwise.common.exception;

public class ImportException extends RuntimeException {
    public ImportException(String message) {
        super(message);
    }

    public ImportException(String message, Throwable cause) {
        super(message, cause);
    }
}
