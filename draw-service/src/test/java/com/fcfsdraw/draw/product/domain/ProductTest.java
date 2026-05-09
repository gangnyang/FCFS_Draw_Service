package com.fcfsdraw.draw.product.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void decreaseRemainingQuantity_marksSoldOutWhenLastStockIsUsed() {
        // given
        Product product = new Product("ticket", 1L, 10_000L);

        // when
        product.decreaseRemainingQuantity();

        // then
        assertThat(product.getRemainingQuantity()).isZero();
        assertThat(product.getStatus()).isEqualTo(ProductStatus.SOLD_OUT);
    }
}
