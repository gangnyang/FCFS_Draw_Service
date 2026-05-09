package com.fcfsdraw.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fcfsdraw.common.exception.BusinessException;
import com.fcfsdraw.payment.dto.PaymentResponse;
import com.fcfsdraw.payment.repository.PaymentTransactionRepository;
import com.fcfsdraw.wallet.repository.WalletRepository;
import com.fcfsdraw.wallet.dto.WalletResponse;
import com.fcfsdraw.wallet.service.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PaymentServiceTest {

    private static final Long USER_ID = 1L;
    private static final long INITIAL_BALANCE = 10_000L;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private PaymentTransactionRepository paymentTransactionRepository;

    @Autowired
    private WalletRepository walletRepository;

    @BeforeEach
    void setUp() {
        paymentTransactionRepository.deleteAll();
        walletRepository.deleteAll();
    }

    @Test
    void pay_decreasesWalletBalance() {
        // given
        walletService.setBalance(USER_ID, INITIAL_BALANCE);

        // when
        PaymentResponse response = paymentService.pay("request-1", USER_ID, 3_000L);

        // then
        assertThat(response.remainingBalance()).isEqualTo(7_000L);
        assertThat(walletService.getBalance(USER_ID).balance()).isEqualTo(7_000L);
    }

    @Test
    void pay_rollsBackWhenBalanceIsInsufficient() {
        // given
        walletService.setBalance(2L, 1_000L);

        // when, then
        assertThatThrownBy(() -> paymentService.pay("request-2", 2L, 2_000L))
                .isInstanceOf(BusinessException.class);
        assertThat(walletService.getBalance(2L).balance()).isEqualTo(1_000L);
        assertThat(paymentTransactionRepository.findByRequestId("request-2")).isEmpty();
    }

    @Test
    void pay_returnsPreviousResultWhenRequestIdIsRepeated() {
        // given
        walletService.setBalance(3L, INITIAL_BALANCE);
        paymentService.pay("request-3", 3L, 4_000L);

        // when
        PaymentResponse repeated = paymentService.pay("request-3", 3L, 4_000L);

        // then
        WalletResponse wallet = walletService.getBalance(3L);
        assertThat(repeated.remainingBalance()).isEqualTo(6_000L);
        assertThat(wallet.balance()).isEqualTo(6_000L);
        assertThat(paymentTransactionRepository.findAll()).hasSize(1);
    }
}
