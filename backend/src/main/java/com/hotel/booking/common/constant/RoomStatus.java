package com.hotel.booking.common.constant;

/**
 * 物理房间房态（对应 PRD BR-13）。
 * VC 空闲可售 / VD 待清洁 / OC 在住 / OOO 维修停用。
 */
public final class RoomStatus {
    public static final String VC = "VC";
    public static final String VD = "VD";
    public static final String OC = "OC";
    public static final String OOO = "OOO";

    private RoomStatus() {
    }
}
