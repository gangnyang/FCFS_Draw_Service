package com.fcfsdraw.draw.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ProductCreateRequest(
        @NotBlank(message = "상품명은 필수입니다.")
        String name,

        @NotNull(message = "총 수량은 필수입니다.")
        @Min(value = 1, message = "총 수량은 1 이상이어야 합니다.")
        Long totalQuantity,

        @NotNull(message = "가격은 필수입니다.")
        @Min(value = 1, message = "가격은 1 이상이어야 합니다.")
        Long price
) {
}
