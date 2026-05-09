package com.fcfsdraw.payment.service;

import com.fcfsdraw.payment.domain.PaymentTransaction;
import com.fcfsdraw.payment.dto.PaymentResponse;
import com.fcfsdraw.payment.repository.PaymentTransactionRepository;
import com.fcfsdraw.wallet.domain.Wallet;
import com.fcfsdraw.wallet.dto.WalletResponse;
import com.fcfsdraw.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final WalletService walletService;
    private final PaymentTransactionRepository paymentTransactionRepository;

    @Transactional
    public PaymentResponse pay(String requestId, Long userId, long price) {
        return paymentTransactionRepository.findByRequestId(requestId)
                .map(this::repeatedPayment)
                .orElseGet(() -> createPayment(requestId, userId, price));
    }

    private PaymentResponse repeatedPayment(PaymentTransaction transaction) {
        WalletResponse wallet = walletService.getBalance(transaction.getUserId());
        log.info("repeated payment request ignored. requestId={}, userId={}", transaction.getRequestId(), transaction.getUserId());
        return PaymentResponse.repeated(transaction, wallet.balance());
    }

    private PaymentResponse createPayment(String requestId, Long userId, long price) {
        Wallet wallet = walletService.deduct(userId, price);
        PaymentTransaction transaction = paymentTransactionRepository.save(new PaymentTransaction(requestId, userId, price));
        log.info("payment completed. requestId={}, userId={}, price={}", requestId, userId, price);
        return PaymentResponse.of(transaction, wallet);
    }
}
