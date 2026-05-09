package com.fcfsdraw.draw.common.exception;

public record ErrorResponse(
        String message,
        String traceId
) {
}
