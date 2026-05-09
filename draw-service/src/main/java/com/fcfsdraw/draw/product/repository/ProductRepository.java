package com.fcfsdraw.draw.product.repository;

import com.fcfsdraw.draw.product.domain.Product;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;

public interface ProductRepository extends JpaRepository<Product, Long> {

    String LOCK_TIMEOUT_HINT = "jakarta.persistence.lock.timeout";
    String LOCK_TIMEOUT_MILLISECONDS = "3000";

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = LOCK_TIMEOUT_HINT, value = LOCK_TIMEOUT_MILLISECONDS))
    Optional<Product> findWithPessimisticLockById(Long id);
}
