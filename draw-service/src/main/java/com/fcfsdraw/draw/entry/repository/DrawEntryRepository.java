package com.fcfsdraw.draw.entry.repository;

import com.fcfsdraw.draw.entry.domain.DrawEntry;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {

    Optional<DrawEntry> findByRequestId(String requestId);

    Optional<DrawEntry> findByProductIdAndUserId(Long productId, Long userId);
}
