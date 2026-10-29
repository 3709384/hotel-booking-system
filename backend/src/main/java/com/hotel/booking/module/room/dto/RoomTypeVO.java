package com.hotel.booking.module.room.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RoomTypeVO {

    private Long id;
    private Long hotelId;
    private String hotelName;
    private String code;
    private String name;
    private String bedType;
    private Integer maxOccupancy;
    private BigDecimal area;
    private BigDecimal basePrice;
    private BigDecimal marketPrice;
    private String description;
    private String coverImage;
    private List<String> amenities;
    private BigDecimal averageScore;
    private Long reviewCount;
}
