package com.fcfsdraw.draw.entry.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fcfsdraw.draw.entry.domain.DrawResult;
import com.fcfsdraw.draw.entry.domain.DrawStatus;
import com.fcfsdraw.draw.entry.dto.DrawReservation;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.payment.client.PaymentClient;
import com.fcfsdraw.draw.payment.client.PaymentFailedException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DrawPaymentSagaServiceTest {

    private final DrawEntryService drawEntryService = mock(DrawEntryService.class);
    private final PaymentClient paymentClient = mock(PaymentClient.class);
    private final DrawPaymentSagaService drawPaymentSagaService = new DrawPaymentSagaService(drawEntryService, paymentClient);

    @Test
    void process_reservesStockThenCallsPaymentThenCompletesPayment() {
        // given
        DrawResponse pending = new DrawResponse("request-1", 1L, 10L, DrawResult.WIN, DrawStatus.PENDING_PAYMENT, null);
        when(drawEntryService.reservePayment("request-1", 1L, 10L))
                .thenReturn(DrawReservation.paymentRequired(pending, 10_000L));

        // when
        drawPaymentSagaService.process("request-1", 1L, 10L);

        // then
        InOrder inOrder = inOrder(drawEntryService, paymentClient);
        inOrder.verify(drawEntryService).reservePayment("request-1", 1L, 10L);
        inOrder.verify(paymentClient).pay("request-1", 10L, 10_000L);
        inOrder.verify(drawEntryService).completePayment("request-1");
    }

    @Test
    void process_compensatesStockWhenPaymentFails() {
        // given
        DrawResponse pending = new DrawResponse("request-1", 1L, 10L, DrawResult.WIN, DrawStatus.PENDING_PAYMENT, null);
        when(drawEntryService.reservePayment("request-1", 1L, 10L))
                .thenReturn(DrawReservation.paymentRequired(pending, 10_000L));
        org.mockito.Mockito.doThrow(new PaymentFailedException("failed", new RuntimeException("boom")))
                .when(paymentClient).pay("request-1", 10L, 10_000L);

        // when
        drawPaymentSagaService.process("request-1", 1L, 10L);

        // then
        InOrder inOrder = inOrder(drawEntryService, paymentClient);
        inOrder.verify(drawEntryService).reservePayment("request-1", 1L, 10L);
        inOrder.verify(paymentClient).pay("request-1", 10L, 10_000L);
        inOrder.verify(drawEntryService).compensatePayment("request-1");
    }
}
