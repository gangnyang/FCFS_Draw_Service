package com.fcfsdraw.draw.common.health;

import com.fcfsdraw.draw.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class HealthController {

    private static final String SERVICE_NAME = "draw-service";
    private static final String STATUS_UP = "UP";

    @GetMapping
    public ApiResponse<HealthResponse> root() {
        return ApiResponse.success(new HealthResponse(SERVICE_NAME, STATUS_UP));
    }

    @GetMapping("/api/v1/health")
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(new HealthResponse(SERVICE_NAME, STATUS_UP));
    }
}
