package com.fcfsdraw.draw.entry.service;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fcfsdraw.draw.entry.dto.DrawQueueItem;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class DrawQueueWorkerTest {

    private final DrawQueueService drawQueueService = mock(DrawQueueService.class);
    private final DrawPaymentSagaService drawPaymentSagaService = mock(DrawPaymentSagaService.class);
    private final DrawQueueWorker drawQueueWorker = new DrawQueueWorker(drawQueueService, drawPaymentSagaService);

    @Test
    void consume_popsTopFiftyAndProcessesDrawSequentially() {
        // given
        when(drawQueueService.activeProductIds()).thenReturn(List.of(1L));
        when(drawQueueService.popFirst(1L, 50)).thenReturn(List.of(
                new DrawQueueItem(1L, 10L),
                new DrawQueueItem(1L, 11L)
        ));

        // when
        drawQueueWorker.consume();

        // then
        InOrder inOrder = inOrder(drawPaymentSagaService);
        inOrder.verify(drawPaymentSagaService).process("queue-1-10", 1L, 10L);
        inOrder.verify(drawPaymentSagaService).process("queue-1-11", 1L, 11L);
    }
}
