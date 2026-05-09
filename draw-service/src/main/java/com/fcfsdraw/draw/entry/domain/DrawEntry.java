package com.fcfsdraw.draw.entry.domain;

import com.fcfsdraw.draw.common.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(
        name = "draw_entries",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_draw_entries_request_id", columnNames = "request_id"),
                @UniqueConstraint(name = "uk_draw_entries_product_user", columnNames = {"product_id", "user_id"})
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DrawEntry extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false, updatable = false, length = 100)
    private String requestId;

    @Column(name = "product_id", nullable = false, updatable = false)
    private Long productId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DrawResult result;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private DrawFailReason failReason;

    public DrawEntry(String requestId, Long productId, Long userId, DrawResult result, DrawFailReason failReason) {
        this.requestId = requestId;
        this.productId = productId;
        this.userId = userId;
        this.result = result;
        this.failReason = failReason;
    }

    public static DrawEntry win(String requestId, Long productId, Long userId) {
        return new DrawEntry(requestId, productId, userId, DrawResult.WIN, null);
    }

    public static DrawEntry lose(String requestId, Long productId, Long userId, DrawFailReason failReason) {
        return new DrawEntry(requestId, productId, userId, DrawResult.LOSE, failReason);
    }

    public boolean hasSameDraw(Long productId, Long userId) {
        return this.productId.equals(productId) && this.userId.equals(userId);
    }
}
