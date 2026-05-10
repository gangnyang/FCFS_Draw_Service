package com.fcfsdraw.draw.payment.client;

import com.fcfsdraw.draw.payment.dto.PaymentRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
public class PaymentClient {

    private static final String PAYMENT_PATH = "/api/v1/payments";

    private final RestTemplate restTemplate;
    private final String paymentBaseUrl;

    public PaymentClient(
            RestTemplate restTemplate,
            @Value("${draw.payment.base-url}") String paymentBaseUrl
    ) {
        this.restTemplate = restTemplate;
        this.paymentBaseUrl = paymentBaseUrl;
    }

    public void pay(String requestId, Long userId, long price) {
        try {
            restTemplate.postForEntity(paymentBaseUrl + PAYMENT_PATH, new PaymentRequest(requestId, userId, price), String.class);
        } catch (RestClientException exception) {
            log.warn("payment request failed. requestId={}, userId={}, price={}", requestId, userId, price, exception);
            throw new PaymentFailedException("payment request failed. requestId=" + requestId, exception);
        }
    }
}
