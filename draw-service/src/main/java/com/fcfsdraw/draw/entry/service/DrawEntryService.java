package com.fcfsdraw.draw.entry.service;

import com.fcfsdraw.draw.common.exception.BusinessException;
import com.fcfsdraw.draw.common.exception.ErrorCode;
import com.fcfsdraw.draw.entry.domain.DrawEntry;
import com.fcfsdraw.draw.entry.domain.DrawFailReason;
import com.fcfsdraw.draw.entry.domain.DrawStatus;
import com.fcfsdraw.draw.entry.dto.DrawReservation;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.entry.repository.DrawEntryRepository;
import com.fcfsdraw.draw.product.domain.Product;
import com.fcfsdraw.draw.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DrawEntryService {

    private final ProductService productService;
    private final DrawEntryRepository drawEntryRepository;

    @Transactional
    public DrawResponse draw(String requestId, Long productId, Long userId) {
        return reservePayment(requestId, productId, userId).response();
    }

    @Transactional
    public DrawReservation reservePayment(String requestId, Long productId, Long userId) {
        return drawEntryRepository.findByRequestId(requestId)
                .map(entry -> repeatedDraw(entry, productId, userId))
                .orElseGet(() -> drawSafely(requestId, productId, userId));
    }

    @Transactional
    public DrawResponse completePayment(String requestId) {
        DrawEntry entry = drawEntryRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        entry.markPaymentSuccess();
        log.info("draw payment completed. requestId={}, productId={}, userId={}", entry.getRequestId(), entry.getProductId(), entry.getUserId());
        return DrawResponse.from(entry);
    }

    @Transactional
    public DrawResponse compensatePayment(String requestId) {
        DrawEntry entry = drawEntryRepository.findByRequestId(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));

        if (entry.isPendingPayment()) {
            Product product = productService.findById(entry.getProductId());
            entry.markPaymentFailed(DrawFailReason.PAYMENT_FAILED);
            product.restoreRemainingQuantity();
            log.info("draw payment compensated. requestId={}, productId={}, userId={}", entry.getRequestId(), entry.getProductId(), entry.getUserId());
        }

        return DrawResponse.from(entry);
    }

    private DrawReservation repeatedDraw(DrawEntry entry, Long productId, Long userId) {
        validateSameDraw(entry, productId, userId);
        log.info("repeated draw request ignored. requestId={}, productId={}, userId={}", entry.getRequestId(), productId, userId);
        return DrawReservation.paymentNotRequired(DrawResponse.from(entry));
    }

    private DrawReservation drawSafely(String requestId, Long productId, Long userId) {
        try {
            return drawEntryRepository.findByProductIdAndUserId(productId, userId)
                    .map(DrawResponse::from)
                    .map(DrawReservation::paymentNotRequired)
                    .orElseGet(() -> createDraw(requestId, productId, userId));
        } catch (DataIntegrityViolationException exception) {
            log.info("duplicated draw request detected during insert. requestId={}, productId={}, userId={}", requestId, productId, userId);
            return drawEntryRepository.findByRequestId(requestId)
                    .map(entry -> repeatedDraw(entry, productId, userId))
                    .or(() -> drawEntryRepository.findByProductIdAndUserId(productId, userId)
                            .map(DrawResponse::from)
                            .map(DrawReservation::paymentNotRequired))
                    .orElseThrow(() -> exception);
        }
    }

    private DrawReservation createDraw(String requestId, Long productId, Long userId) {
        Product product = productService.findById(productId);

        if (!product.isDrawable()) {
            return DrawReservation.paymentNotRequired(DrawResponse.from(drawEntryRepository.saveAndFlush(
                    DrawEntry.lose(requestId, productId, userId, DrawFailReason.SOLD_OUT)
            )));
        }

        product.decreaseRemainingQuantity();
        DrawEntry entry = drawEntryRepository.saveAndFlush(DrawEntry.pendingPayment(requestId, productId, userId));
        log.info("draw reserved. requestId={}, productId={}, userId={}, status={}", requestId, productId, userId, DrawStatus.PENDING_PAYMENT);
        return DrawReservation.paymentRequired(DrawResponse.from(entry), product.getPrice());
    }

    private void validateSameDraw(DrawEntry entry, Long productId, Long userId) {
        if (!entry.hasSameDraw(productId, userId)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }
}
