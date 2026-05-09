package com.fcfsdraw.common.exception;

public record ErrorResponse(
        String message,
        String traceId
) {
}
