package com.fcfsdraw.draw.entry.facade;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fcfsdraw.draw.common.exception.ConcurrencyFailureException;
import com.fcfsdraw.draw.entry.domain.DrawResult;
import com.fcfsdraw.draw.entry.dto.DrawResponse;
import com.fcfsdraw.draw.entry.service.DrawEntryService;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

class DrawEntryFacadeTest {

    private final RedissonClient redissonClient = mock(RedissonClient.class);
    private final RLock lock = mock(RLock.class);
    private final DrawEntryService drawEntryService = mock(DrawEntryService.class);
    private final DrawEntryFacade drawEntryFacade = new DrawEntryFacade(redissonClient, drawEntryService);

    @Test
    void draw_locksProductAndUnlocksAfterServiceReturns() throws InterruptedException {
        // given
        DrawResponse expected = new DrawResponse("request-1", 10L, 20L, DrawResult.WIN, null);
        when(redissonClient.getLock("product:10")).thenReturn(lock);
        when(lock.tryLock(100L, 10_000L, MILLISECONDS)).thenReturn(true);
        when(lock.isHeldByCurrentThread()).thenReturn(true);
        when(drawEntryService.draw("request-1", 10L, 20L)).thenReturn(expected);

        // when
        DrawResponse response = drawEntryFacade.draw("request-1", 10L, 20L);

        // then
        assertThat(response).isEqualTo(expected);
        InOrder inOrder = inOrder(lock, drawEntryService);
        inOrder.verify(lock).tryLock(100L, 10_000L, MILLISECONDS);
        inOrder.verify(drawEntryService).draw("request-1", 10L, 20L);
        inOrder.verify(lock).unlock();
    }

    @Test
    void draw_throwsConcurrencyFailureWhenLockIsNotAcquired() throws InterruptedException {
        // given
        when(redissonClient.getLock("product:10")).thenReturn(lock);
        when(lock.tryLock(100L, 10_000L, MILLISECONDS)).thenReturn(false);

        // when, then
        assertThatThrownBy(() -> drawEntryFacade.draw("request-1", 10L, 20L))
                .isInstanceOf(ConcurrencyFailureException.class);

        verify(drawEntryService, never()).draw("request-1", 10L, 20L);
        verify(lock, never()).unlock();
    }
}
