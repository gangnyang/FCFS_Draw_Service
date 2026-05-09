package com.fcfsdraw.draw.entry.facade;

import com.fcfsdraw.draw.common.exception.ConcurrencyFailureException;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.entry.service.DrawEntryService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DrawEntryFacade {

    private static final long LOCK_WAIT_TIME = 100L;
    private static final long LOCK_LEASE_TIME = 10_000L;
    private static final TimeUnit LOCK_TIME_UNIT = TimeUnit.MILLISECONDS;

    private final RedissonClient redissonClient;
    private final DrawEntryService drawEntryService;

    public DrawResponse draw(String requestId, Long productId, Long userId) {
        String lockName = productLockName(productId);
        RLock lock = redissonClient.getLock(lockName);
        boolean locked = false;

        try {
            // Step 3: Redis 분산 락으로 상품 단위 진입을 짧게 제어하고, 실패 시 빠르게 응답한다.
            locked = lock.tryLock(LOCK_WAIT_TIME, LOCK_LEASE_TIME, LOCK_TIME_UNIT);
            if (!locked) {
                throw new ConcurrencyFailureException("draw lock acquisition failed. lockName=" + lockName);
            }

            return drawEntryService.draw(requestId, productId, userId);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConcurrencyFailureException("draw lock acquisition interrupted. lockName=" + lockName, exception);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    private String productLockName(Long productId) {
        return "product:" + productId;
    }
}
