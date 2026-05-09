package com.fcfsdraw.draw.product.dto;

import com.fcfsdraw.draw.product.domain.Product;
import com.fcfsdraw.draw.product.domain.ProductStatus;

public record ProductResponse(
        Long productId,
        String name,
        long totalQuantity,
        long remainingQuantity,
        long price,
        ProductStatus status
) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getTotalQuantity(),
                product.getRemainingQuantity(),
                product.getPrice(),
                product.getStatus()
        );
    }
}
