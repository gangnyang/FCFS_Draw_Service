package com.fcfsdraw.draw.entry.dto;

public record DrawQueueResponse(
        Long productId,
        Long userId,
        Long rank,
        String status
) {

    public static DrawQueueResponse waiting(Long productId, Long userId, Long rank) {
        return new DrawQueueResponse(productId, userId, rank, "WAITING");
    }

    public static DrawQueueResponse notQueued(Long productId, Long userId) {
        return new DrawQueueResponse(productId, userId, null, "NOT_QUEUED");
    }
}
