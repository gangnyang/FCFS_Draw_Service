package com.fcfsdraw.draw.product.repository;

import com.fcfsdraw.draw.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
