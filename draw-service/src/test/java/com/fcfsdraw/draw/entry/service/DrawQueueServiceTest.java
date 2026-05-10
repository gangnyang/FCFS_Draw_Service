package com.fcfsdraw.draw.entry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fcfsdraw.draw.entry.dto.DrawQueueItem;
import com.fcfsdraw.draw.entry.dto.DrawQueueResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.redisson.api.RScoredSortedSet;
import org.redisson.api.RSet;
import org.redisson.api.RedissonClient;
import org.redisson.client.protocol.ScoredEntry;

@SuppressWarnings("unchecked")
class DrawQueueServiceTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RScoredSortedSet<String> queue = mock(RScoredSortedSet.class);
    private final RSet<String> activeProducts = mock(RSet.class);
    private final DrawQueueService drawQueueService = new DrawQueueService(redissonClient);

    @Test
    void enqueue_addsUserToProductQueueAndReturnsOneBasedRank() {
        // given
        when(redissonClient.<String>getScoredSortedSet("draw:queue:1")).thenReturn(queue);
        when(redissonClient.<String>getSet("draw:queue:products")).thenReturn(activeProducts);
        when(queue.rank("10")).thenReturn(0);

        // when
        DrawQueueResponse response = drawQueueService.enqueue(1L, 10L);

        // then
        assertThat(response.productId()).isEqualTo(1L);
        assertThat(response.userId()).isEqualTo(10L);
        assertThat(response.rank()).isEqualTo(1L);
        assertThat(response.status()).isEqualTo("WAITING");
    }

    @Test
    void getRank_returnsNotQueuedWhenUserIsMissing() {
        // given
        when(redissonClient.<String>getScoredSortedSet("draw:queue:1")).thenReturn(queue);
        when(queue.rank("10")).thenReturn(null);

        // when
        DrawQueueResponse response = drawQueueService.getRank(1L, 10L);

        // then
        assertThat(response.status()).isEqualTo("NOT_QUEUED");
        assertThat(response.rank()).isNull();
    }

    @Test
    void popFirst_returnsQueueItemsInScoreOrder() {
        // given
        when(redissonClient.<String>getScoredSortedSet("draw:queue:1")).thenReturn(queue);
        when(redissonClient.<String>getSet("draw:queue:products")).thenReturn(activeProducts);
        when(queue.pollFirstEntries(2)).thenReturn(List.of(
                new ScoredEntry<>(100.0, "10"),
                new ScoredEntry<>(101.0, "11")
        ));
        when(queue.isEmpty()).thenReturn(true);

        // when
        List<DrawQueueItem> items = drawQueueService.popFirst(1L, 2);

        // then
        assertThat(items).extracting(DrawQueueItem::userId).containsExactly(10L, 11L);
    }
}
