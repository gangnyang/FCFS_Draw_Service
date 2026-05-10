package com.fcfsdraw.draw.product.domain;

import com.fcfsdraw.draw.common.domain.BaseEntity;
import com.fcfsdraw.draw.common.exception.BusinessException;
import com.fcfsdraw.draw.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "products")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    private static final long MINIMUM_QUANTITY = 0L;
    private static final long MINIMUM_PRICE = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private long totalQuantity;

    @Column(nullable = false)
    private long remainingQuantity;

    @Column(nullable = false)
    private long price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ProductStatus status;

    public Product(String name, long totalQuantity, long price) {
        validateQuantity(totalQuantity);
        validatePrice(price);
        this.name = name;
        this.totalQuantity = totalQuantity;
        this.remainingQuantity = totalQuantity;
        this.price = price;
        this.status = ProductStatus.OPEN;
    }

    public boolean isDrawable() {
        return status == ProductStatus.OPEN && remainingQuantity > MINIMUM_QUANTITY;
    }

    public void decreaseRemainingQuantity() {
        if (!isDrawable()) {
            markSoldOutIfNeeded();
            return;
        }

        remainingQuantity -= 1;
        markSoldOutIfNeeded();
    }

    public void restoreRemainingQuantity() {
        if (remainingQuantity < totalQuantity) {
            remainingQuantity += 1;
            status = ProductStatus.OPEN;
        }
    }

    private void markSoldOutIfNeeded() {
        if (remainingQuantity <= MINIMUM_QUANTITY) {
            status = ProductStatus.SOLD_OUT;
        }
    }

    private void validateQuantity(long quantity) {
        if (quantity <= MINIMUM_QUANTITY) {
            throw new BusinessException(ErrorCode.INVALID_QUANTITY);
        }
    }

    private void validatePrice(long price) {
        if (price <= MINIMUM_PRICE) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
    }
}
