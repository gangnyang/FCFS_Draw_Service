package com.fcfsdraw.draw.entry.controller;

import com.fcfsdraw.draw.common.response.ApiResponse;
import com.fcfsdraw.draw.entry.dto.DrawRequest;
import com.fcfsdraw.draw.entry.dto.DrawQueueResponse;
import com.fcfsdraw.draw.entry.service.DrawQueueService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping({"/api/v1/draws", "/draw"})
public class DrawEntryController {

    private final DrawQueueService drawQueueService;

    @PostMapping
    public ResponseEntity<ApiResponse<DrawQueueResponse>> draw(@Valid @RequestBody DrawRequest request) {
        DrawQueueResponse response = drawQueueService.enqueue(request.productId(), request.userId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.success(response));
    }

    @GetMapping("/queue/rank")
    public ApiResponse<DrawQueueResponse> rank(
            @RequestParam @Positive(message = "상품 ID는 0보다 커야 합니다.") Long productId,
            @RequestParam @Positive(message = "사용자 ID는 0보다 커야 합니다.") Long userId
    ) {
        return ApiResponse.success(drawQueueService.getRank(productId, userId));
    }
}
