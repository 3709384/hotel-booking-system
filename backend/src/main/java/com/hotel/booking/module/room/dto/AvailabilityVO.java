package com.hotel.booking.module.room.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 可售查询结果：某房型在所选区间的每夜最小可售量与"一间住 N 晚"总价。
 */
@Data
public class AvailabilityVO {

    private Long roomTypeId;
    private String code;
    private String name;
    private String bedType;
    private BigDecimal basePrice;
    private long nights;
    /** 区间内每夜最小可售间数；0 表示不可订 */
    private Integer minAvailable;
    /** 一间房住 nights 晚的总价 */
    private BigDecimal totalPrice;
    private List<String> amenities;
}
