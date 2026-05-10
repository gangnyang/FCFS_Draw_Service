package com.fcfsdraw.draw.entry.service;

import com.fcfsdraw.draw.entry.dto.DrawReservation;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.payment.client.PaymentFailedException;
import com.fcfsdraw.draw.payment.client.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawPaymentSagaService {

    private final DrawEntryService drawEntryService;
    private final PaymentClient paymentClient;

    public DrawResponse process(String requestId, Long productId, Long userId) {
        DrawReservation reservation = drawEntryService.reservePayment(requestId, productId, userId);
        if (!reservation.requiresPayment()) {
            return reservation.response();
        }

        try {
            // 외부 결제 호출은 DB 트랜잭션을 잡지 않은 상태에서 수행한다.
            paymentClient.pay(requestId, userId, reservation.price());
            return drawEntryService.completePayment(requestId);
        } catch (PaymentFailedException exception) {
            log.warn("payment failed. compensating draw reservation. requestId={}, productId={}, userId={}", requestId, productId, userId);
            return drawEntryService.compensatePayment(requestId);
        }
    }
}
