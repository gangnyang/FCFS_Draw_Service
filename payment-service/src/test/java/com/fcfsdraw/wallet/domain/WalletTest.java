package com.fcfsdraw.wallet.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fcfsdraw.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

class WalletTest {

    @Test
    void deduct_decreasesBalanceWhenBalanceIsEnough() {
        // given
        Wallet wallet = new Wallet(1L, 10_000L);

        // when
        wallet.deduct(2_500L);

        // then
        assertThat(wallet.getBalance()).isEqualTo(7_500L);
    }

    @Test
    void deduct_throwsExceptionWhenBalanceIsInsufficient() {
        // given
        Wallet wallet = new Wallet(1L, 1_000L);

        // when, then
        assertThatThrownBy(() -> wallet.deduct(2_000L))
                .isInstanceOf(BusinessException.class);
    }
}
