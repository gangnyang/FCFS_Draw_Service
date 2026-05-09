package com.fcfsdraw.common.health;

public record HealthResponse(
        String service,
        String status
) {
}
