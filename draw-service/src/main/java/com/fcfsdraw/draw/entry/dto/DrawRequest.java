package com.fcfsdraw.draw.entry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record DrawRequest(
        @NotBlank(message = "요청 ID는 필수입니다.")
        String requestId,

        @NotNull(message = "상품 ID는 필수입니다.")
        @Positive(message = "상품 ID는 0보다 커야 합니다.")
        Long productId,

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID는 0보다 커야 합니다.")
        Long userId
) {
}
