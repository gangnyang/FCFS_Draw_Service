package com.fcfsdraw.draw.payment.dto;

public record PaymentRequest(
        String requestId,
        Long userId,
        Long price
) {
}
