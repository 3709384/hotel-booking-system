package com.hotel.booking.common.constant;

/**
 * 订单状态机（对应 PRD BR-10 / HLD 状态图）。
 */
public final class ReservationStatus {
    public static final String PENDING_PAYMENT = "PENDING_PAYMENT";
    public static final String CONFIRMED = "CONFIRMED";
    public static final String CHECKED_IN = "CHECKED_IN";
    public static final String COMPLETED = "COMPLETED";
    public static final String CANCELLED = "CANCELLED";
    public static final String NO_SHOW = "NO_SHOW";
    public static final String REFUNDING = "REFUNDING";
    public static final String REFUNDED = "REFUNDED";

    private ReservationStatus() {
    }
}
