package com.fcfsdraw.draw.common.exception;

public class ConcurrencyFailureException extends RuntimeException {

    public ConcurrencyFailureException(String message) {
        super(message);
    }

    public ConcurrencyFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
