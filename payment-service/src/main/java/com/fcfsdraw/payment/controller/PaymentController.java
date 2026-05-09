package com.fcfsdraw.payment.controller;

import com.fcfsdraw.common.response.ApiResponse;
import com.fcfsdraw.payment.dto.PaymentRequest;
import com.fcfsdraw.payment.dto.PaymentResponse;
import com.fcfsdraw.payment.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ApiResponse<PaymentResponse> pay(@Valid @RequestBody PaymentRequest request) {
        return ApiResponse.success(paymentService.pay(request.requestId(), request.userId(), request.price()));
    }
}
