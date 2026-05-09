package com.fcfsdraw.draw.product.controller;

import com.fcfsdraw.draw.common.response.ApiResponse;
import com.fcfsdraw.draw.product.dto.ProductCreateRequest;
import com.fcfsdraw.draw.product.dto.ProductResponse;
import com.fcfsdraw.draw.product.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ApiResponse<ProductResponse> create(@Valid @RequestBody ProductCreateRequest request) {
        return ApiResponse.success(productService.create(request.name(), request.totalQuantity(), request.price()));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductResponse> get(
            @PathVariable @Positive(message = "상품 ID는 0보다 커야 합니다.") Long productId
    ) {
        return ApiResponse.success(productService.get(productId));
    }
}
