package com.fcfsdraw.draw.entry.service;

import com.fcfsdraw.draw.entry.dto.DrawQueueItem;
import com.fcfsdraw.draw.entry.dto.DrawQueueResponse;
import java.util.List;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;
import org.springframework.stereotype.Service;

@Service
public class DrawQueueService {

    private static final String QUEUE_KEY_PREFIX = "draw:queue:";
    private static final String ACTIVE_PRODUCT_KEY = "draw:queue:products";

    private final RedissonClient redissonClient;

    public DrawQueueService(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public DrawQueueResponse enqueue(Long productId, Long userId) {
        queue(productId).addIfAbsent(System.currentTimeMillis(), queueValue(userId));
        activeProducts().add(String.valueOf(productId));
        return getRank(productId, userId);
    }

    public DrawQueueResponse getRank(Long productId, Long userId) {
        Integer zeroBasedRank = queue(productId).rank(queueValue(userId));
        if (zeroBasedRank == null) {
            return DrawQueueResponse.notQueued(productId, userId);
        }
        return DrawQueueResponse.waiting(productId, userId, zeroBasedRank.longValue() + 1L);
    }

    public List<Long> activeProductIds() {
        return activeProducts().stream()
                .map(Long::valueOf)
                .toList();
    }

    public List<DrawQueueItem> popFirst(Long productId, int count) {
        List<ScoredEntry<String>> entries = queue(productId).pollFirstEntries(count);
        if (queue(productId).isEmpty()) {
            activeProducts().remove(String.valueOf(productId));
        }

        return entries.stream()
                .map(entry -> new DrawQueueItem(productId, Long.valueOf(entry.getValue())))
                .toList();
    }

    private RScoredSortedSet<String> queue(Long productId) {
        return redissonClient.getScoredSortedSet(QUEUE_KEY_PREFIX + productId);
    }

    private RSet<String> activeProducts() {
        return redissonClient.getSet(ACTIVE_PRODUCT_KEY);
    }

    private String queueValue(Long userId) {
        return String.valueOf(userId);
    }
}
