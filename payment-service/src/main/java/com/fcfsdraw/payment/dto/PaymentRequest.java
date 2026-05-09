package com.fcfsdraw.payment.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotBlank(message = "요청 ID는 필수입니다.")
        String requestId,

        @NotNull(message = "사용자 ID는 필수입니다.")
        @Positive(message = "사용자 ID는 0보다 커야 합니다.")
        Long userId,

        @NotNull(message = "구매 금액은 필수입니다.")
        @Min(value = 1, message = "구매 금액은 1 이상이어야 합니다.")
        Long price
) {
}
