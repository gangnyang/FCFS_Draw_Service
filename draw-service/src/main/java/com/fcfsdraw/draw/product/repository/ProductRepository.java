package com.fcfsdraw.draw.product.repository;

import com.fcfsdraw.draw.product.domain.Product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("""
            select p.id as productId,
                   p.totalQuantity as totalQuantity,
                   p.remainingQuantity as remainingQuantity
            from Product p
            """)
    List<ProductMetricView> findProductMetricViews();

    interface ProductMetricView {

        Long getProductId();

        long getTotalQuantity();

        long getRemainingQuantity();
    }
}
