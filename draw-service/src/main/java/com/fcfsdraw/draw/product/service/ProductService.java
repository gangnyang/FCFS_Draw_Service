package com.fcfsdraw.draw.product.service;

import com.fcfsdraw.draw.common.exception.BusinessException;
import com.fcfsdraw.draw.common.exception.ErrorCode;
import com.fcfsdraw.draw.product.domain.Product;
import com.fcfsdraw.draw.product.dto.ProductResponse;
import com.fcfsdraw.draw.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(String name, long totalQuantity, long price) {
        Product product = productRepository.save(new Product(name, totalQuantity, price));
        return ProductResponse.from(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse get(Long productId) {
        return ProductResponse.from(findById(productId));
    }

    public Product findById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
    }
}
