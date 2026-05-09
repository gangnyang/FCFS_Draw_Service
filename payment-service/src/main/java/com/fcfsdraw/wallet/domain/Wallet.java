package com.fcfsdraw.wallet.domain;

import com.fcfsdraw.common.domain.BaseEntity;
import com.fcfsdraw.common.exception.BusinessException;
import com.fcfsdraw.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
        name = "wallets",
        uniqueConstraints = @UniqueConstraint(name = "uk_wallets_user_id", columnNames = "user_id")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Wallet extends BaseEntity {

    private static final long MINIMUM_AMOUNT = 0L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(nullable = false)
    private long balance;

    public Wallet(Long userId, long balance) {
        validatePositiveOrZero(balance);
        this.userId = userId;
        this.balance = balance;
    }

    public void changeBalance(long balance) {
        validatePositiveOrZero(balance);
        this.balance = balance;
    }

    public void deduct(long price) {
        validatePositive(price);
        if (balance < price) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        balance -= price;
    }

    private void validatePositive(long amount) {
        if (amount <= MINIMUM_AMOUNT) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
    }

    private void validatePositiveOrZero(long amount) {
        if (amount < MINIMUM_AMOUNT) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
    }
}
