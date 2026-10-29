package com.hotel.booking.module.hotel.dto;

import com.hotel.booking.module.room.dto.RoomTypeVO;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 酒店列表/详情展示对象。
 * fromPrice 为该酒店在售房型的最低价（列表页「¥xx 起」），marketPrice 为其划线价。
 * roomTypes 仅详情接口填充。
 */
@Data
public class HotelVO {

    private Long id;
    private String name;
    private String kind;
    private BigDecimal score;
    private String scoreWord;
    private String quote;
    private List<String> tags;
    private BigDecimal distanceKm;
    private String distanceText;
    private String landmark;
    private Integer streak;
    private String salesText;
    private String badge;
    private String promoType;
    private String promoText;
    private Integer member;
    private String coverImage;
    private String icon;

    /** 在售房型最低价（起价） */
    private BigDecimal fromPrice;
    /** 起价房型的划线价 */
    private BigDecimal marketPrice;
    /** 该酒店全部房型的点评总数 */
    private Long reviewCount;

    private List<RoomTypeVO> roomTypes;
}
