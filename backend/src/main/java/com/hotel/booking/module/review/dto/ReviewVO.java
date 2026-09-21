package com.hotel.booking.module.review.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReviewVO {
    private Long id;
    private String userName;
    private Integer score;
    private String content;
    private LocalDateTime createdAt;
}
