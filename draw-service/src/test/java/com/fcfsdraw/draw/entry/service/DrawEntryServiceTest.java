package com.fcfsdraw.draw.entry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fcfsdraw.draw.common.exception.BusinessException;
import com.fcfsdraw.draw.common.exception.ErrorCode;
import com.fcfsdraw.draw.entry.domain.DrawFailReason;
import com.fcfsdraw.draw.entry.domain.DrawResult;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.entry.repository.DrawEntryRepository;
import com.fcfsdraw.draw.product.repository.ProductRepository;
import com.fcfsdraw.draw.product.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class DrawEntryServiceTest {

    private final DrawEntryService drawEntryService;
    private final ProductService productService;
    private final DrawEntryRepository drawEntryRepository;
    private final ProductRepository productRepository;

    @Autowired
    DrawEntryServiceTest(
            DrawEntryService drawEntryService,
            ProductService productService,
            DrawEntryRepository drawEntryRepository,
            ProductRepository productRepository
    ) {
        this.drawEntryService = drawEntryService;
        this.productService = productService;
        this.drawEntryRepository = drawEntryRepository;
        this.productRepository = productRepository;
    }

    @BeforeEach
    void setUp() {
        drawEntryRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void draw_returnsWinWhenStockRemains() {
        // given
        Long productId = productService.create("ticket", 1L, 10_000L).productId();

        // when
        DrawResponse response = drawEntryService.draw("draw-1", productId, 1L);

        // then
        assertThat(response.result()).isEqualTo(DrawResult.WIN);
        assertThat(productService.get(productId).remainingQuantity()).isZero();
    }

    @Test
    void draw_returnsLoseWhenStockIsSoldOut() {
        // given
        Long productId = productService.create("ticket", 1L, 10_000L).productId();
        drawEntryService.draw("draw-2", productId, 1L);

        // when
        DrawResponse response = drawEntryService.draw("draw-3", productId, 2L);

        // then
        assertThat(response.result()).isEqualTo(DrawResult.LOSE);
        assertThat(response.failReason()).isEqualTo(DrawFailReason.SOLD_OUT);
    }

    @Test
    void draw_returnsPreviousResultWhenRequestIdIsRepeated() {
        // given
        Long productId = productService.create("ticket", 2L, 10_000L).productId();
        drawEntryService.draw("draw-4", productId, 1L);

        // when
        DrawResponse repeated = drawEntryService.draw("draw-4", productId, 1L);

        // then
        assertThat(repeated.result()).isEqualTo(DrawResult.WIN);
        assertThat(productService.get(productId).remainingQuantity()).isEqualTo(1L);
        assertThat(drawEntryRepository.findAll()).hasSize(1);
    }

    @Test
    void draw_throwsExceptionWhenRepeatedRequestHasDifferentPayload() {
        // given
        Long productId = productService.create("ticket", 2L, 10_000L).productId();
        drawEntryService.draw("draw-5", productId, 1L);

        // when, then
        assertThatThrownBy(() -> drawEntryService.draw("draw-5", productId, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.IDEMPOTENCY_CONFLICT)
                );
    }

    @Test
    void draw_returnsPreviousUserResultWhenSameUserEntersAgainWithDifferentRequestId() {
        // given
        Long productId = productService.create("ticket", 2L, 10_000L).productId();
        drawEntryService.draw("draw-6", productId, 1L);

        // when
        DrawResponse repeatedUser = drawEntryService.draw("draw-7", productId, 1L);

        // then
        assertThat(repeatedUser.result()).isEqualTo(DrawResult.WIN);
        assertThat(productService.get(productId).remainingQuantity()).isEqualTo(1L);
        assertThat(drawEntryRepository.findAll()).hasSize(1);
    }
}
