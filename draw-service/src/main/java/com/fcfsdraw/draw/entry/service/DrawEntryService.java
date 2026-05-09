package com.fcfsdraw.draw.entry.service;

import com.fcfsdraw.draw.common.exception.BusinessException;
import com.fcfsdraw.draw.common.exception.ErrorCode;
import com.fcfsdraw.draw.entry.domain.DrawEntry;
import com.fcfsdraw.draw.entry.domain.DrawFailReason;
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
        return drawEntryRepository.findByRequestId(requestId)
                .map(entry -> repeatedDraw(entry, productId, userId))
                .orElseGet(() -> drawSafely(requestId, productId, userId));
    }

    private DrawResponse repeatedDraw(DrawEntry entry, Long productId, Long userId) {
        validateSameDraw(entry, productId, userId);
        log.info("repeated draw request ignored. requestId={}, productId={}, userId={}", entry.getRequestId(), productId, userId);
        return DrawResponse.from(entry);
    }

    private DrawResponse drawSafely(String requestId, Long productId, Long userId) {
        try {
            return drawEntryRepository.findByProductIdAndUserId(productId, userId)
                    .map(DrawResponse::from)
                    .orElseGet(() -> createDraw(requestId, productId, userId));
        } catch (DataIntegrityViolationException exception) {
            log.info("duplicated draw request detected during insert. requestId={}, productId={}, userId={}", requestId, productId, userId);
            return drawEntryRepository.findByRequestId(requestId)
                    .map(entry -> repeatedDraw(entry, productId, userId))
                    .or(() -> drawEntryRepository.findByProductIdAndUserId(productId, userId).map(DrawResponse::from))
                    .orElseThrow(() -> exception);
        }
    }

    private DrawResponse createDraw(String requestId, Long productId, Long userId) {
        Product product = productService.findById(productId);

        if (!product.isDrawable()) {
            return DrawResponse.from(drawEntryRepository.saveAndFlush(
                    DrawEntry.lose(requestId, productId, userId, DrawFailReason.SOLD_OUT)
            ));
        }

        product.decreaseRemainingQuantity();
        DrawEntry entry = drawEntryRepository.saveAndFlush(DrawEntry.win(requestId, productId, userId));
        log.info("draw completed. requestId={}, productId={}, userId={}, result={}", requestId, productId, userId, entry.getResult());
        return DrawResponse.from(entry);
    }

    private void validateSameDraw(DrawEntry entry, Long productId, Long userId) {
        if (!entry.hasSameDraw(productId, userId)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }
}
