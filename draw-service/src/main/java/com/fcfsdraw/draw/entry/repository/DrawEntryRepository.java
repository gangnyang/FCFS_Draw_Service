package com.fcfsdraw.draw.entry.repository;

import com.fcfsdraw.draw.entry.domain.DrawEntry;
import com.fcfsdraw.draw.entry.domain.DrawStatus;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DrawEntryRepository extends JpaRepository<DrawEntry, Long> {

    Optional<DrawEntry> findByRequestId(String requestId);

    Optional<DrawEntry> findByProductIdAndUserId(Long productId, Long userId);

    @Query("""
            select e.productId as productId, e.status as status, count(e) as count
            from DrawEntry e
            group by e.productId, e.status
            """)
    List<DrawEntryStatusCount> countByProductIdAndStatus();

    interface DrawEntryStatusCount {

        Long getProductId();

        DrawStatus getStatus();

        long getCount();
    }
}
