package com.fcfsdraw.draw.entry.dto;

public record DrawReservation(
        DrawResponse response,
        long price,
        boolean requiresPayment
) {

    public static DrawReservation paymentRequired(DrawResponse response, long price) {
        return new DrawReservation(response, price, true);
    }

    public static DrawReservation paymentNotRequired(DrawResponse response) {
        return new DrawReservation(response, 0L, false);
    }
}
