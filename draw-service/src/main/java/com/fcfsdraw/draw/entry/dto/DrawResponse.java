package com.fcfsdraw.draw.entry.dto;

import com.fcfsdraw.draw.entry.domain.DrawEntry;
import com.fcfsdraw.draw.entry.domain.DrawFailReason;
import com.fcfsdraw.draw.entry.domain.DrawResult;
import com.fcfsdraw.draw.entry.domain.DrawStatus;

public record DrawResponse(
        String requestId,
        Long productId,
        Long userId,
        DrawResult result,
        DrawStatus status,
        DrawFailReason failReason
) {

    public static DrawResponse from(DrawEntry entry) {
        return new DrawResponse(
                entry.getRequestId(),
                entry.getProductId(),
                entry.getUserId(),
                entry.getResult(),
                entry.getStatus(),
                entry.getFailReason()
        );
    }
}
