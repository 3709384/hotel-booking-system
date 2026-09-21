package com.hotel.booking.module.review.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReviewSummaryVO {
    private BigDecimal averageScore;
    private Long reviewCount;
}
