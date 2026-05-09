package com.fcfsdraw.payment.service;

import com.fcfsdraw.common.exception.BusinessException;
import com.fcfsdraw.common.exception.ErrorCode;
import com.fcfsdraw.payment.domain.PaymentTransaction;
import com.fcfsdraw.payment.dto.PaymentResponse;
import com.fcfsdraw.payment.repository.PaymentTransactionRepository;
import com.fcfsdraw.wallet.domain.Wallet;
import com.fcfsdraw.wallet.dto.WalletResponse;
import com.fcfsdraw.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
                .map(transaction -> repeatedPayment(transaction, userId, price))
                .orElseGet(() -> createPaymentSafely(requestId, userId, price));
    }

    private PaymentResponse repeatedPayment(PaymentTransaction transaction, Long userId, long price) {
        validateSamePayment(transaction, userId, price);
        WalletResponse wallet = walletService.getBalance(transaction.getUserId());
        log.info("repeated payment request ignored. requestId={}, userId={}", transaction.getRequestId(), transaction.getUserId());
        return PaymentResponse.repeated(transaction, wallet.balance());
    }

    private PaymentResponse createPaymentSafely(String requestId, Long userId, long price) {
        try {
            return createPayment(requestId, userId, price);
        } catch (DataIntegrityViolationException exception) {
            log.info("duplicated payment request detected during insert. requestId={}, userId={}", requestId, userId);
            return paymentTransactionRepository.findByRequestId(requestId)
                    .map(transaction -> repeatedPayment(transaction, userId, price))
                    .orElseThrow(() -> exception);
        }
    }

    private PaymentResponse createPayment(String requestId, Long userId, long price) {
        Wallet wallet = walletService.deduct(userId, price);
        PaymentTransaction transaction = paymentTransactionRepository.saveAndFlush(new PaymentTransaction(requestId, userId, price));
        log.info("payment completed. requestId={}, userId={}, price={}", requestId, userId, price);
        return PaymentResponse.of(transaction, wallet);
    }

    private void validateSamePayment(PaymentTransaction transaction, Long userId, long price) {
        if (!transaction.hasSamePayment(userId, price)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_CONFLICT);
        }
    }
}
