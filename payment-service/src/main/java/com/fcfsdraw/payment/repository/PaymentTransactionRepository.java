package com.fcfsdraw.payment.repository;

import com.fcfsdraw.payment.domain.PaymentTransaction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    Optional<PaymentTransaction> findByRequestId(String requestId);
}
