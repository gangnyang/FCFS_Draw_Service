package com.fcfsdraw.draw.entry.service;

import com.fcfsdraw.draw.entry.dto.DrawQueueItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "draw.queue.worker.enabled", havingValue = "true", matchIfMissing = true)
public class DrawQueueWorker {

    private static final int BATCH_SIZE = 200;

    private final DrawQueueService drawQueueService;
    private final DrawPaymentSagaService drawPaymentSagaService;

    @Scheduled(fixedDelay = 500)
    public void consume() {
        for (Long productId : drawQueueService.activeProductIds()) {
            processProductQueue(productId);
        }
    }

    private void processProductQueue(Long productId) {
        for (DrawQueueItem item : drawQueueService.popFirst(productId, BATCH_SIZE)) {
            String requestId = "queue-" + item.productId() + "-" + item.userId();
            try {
                drawPaymentSagaService.process(requestId, item.productId(), item.userId());
            } catch (RuntimeException exception) {
                log.warn("failed to process queued draw. productId={}, userId={}", item.productId(), item.userId(), exception);
            }
        }
    }
}
