package com.fcfsdraw.payment.dto;

import com.fcfsdraw.payment.domain.PaymentStatus;
import com.fcfsdraw.payment.domain.PaymentTransaction;
import com.fcfsdraw.wallet.domain.Wallet;

public record PaymentResponse(
        String requestId,
        Long userId,
        long price,
        long remainingBalance,
        PaymentStatus status
) {

    public static PaymentResponse of(PaymentTransaction transaction, Wallet wallet) {
        return new PaymentResponse(
                transaction.getRequestId(),
                transaction.getUserId(),
                transaction.getPrice(),
                wallet.getBalance(),
                transaction.getStatus()
        );
    }

    public static PaymentResponse repeated(PaymentTransaction transaction, long currentBalance) {
        return new PaymentResponse(
                transaction.getRequestId(),
                transaction.getUserId(),
                transaction.getPrice(),
                currentBalance,
                transaction.getStatus()
        );
    }
}
