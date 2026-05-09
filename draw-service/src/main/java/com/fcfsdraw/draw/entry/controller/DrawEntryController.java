package com.fcfsdraw.draw.entry.controller;

import com.fcfsdraw.draw.common.response.ApiResponse;
import com.fcfsdraw.draw.entry.dto.DrawRequest;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.entry.service.DrawEntryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/draws")
public class DrawEntryController {

    private final DrawEntryService drawEntryService;

    @PostMapping
    public ApiResponse<DrawResponse> draw(@Valid @RequestBody DrawRequest request) {
        return ApiResponse.success(drawEntryService.draw(request.requestId(), request.productId(), request.userId()));
    }
}
