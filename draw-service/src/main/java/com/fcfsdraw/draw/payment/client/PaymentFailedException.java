package com.fcfsdraw.draw.payment.client;

public class PaymentFailedException extends RuntimeException {

    public PaymentFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
