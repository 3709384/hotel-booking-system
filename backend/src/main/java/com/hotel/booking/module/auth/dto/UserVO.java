package com.hotel.booking.module.auth.dto;

import lombok.Data;

@Data
public class UserVO {

    private Long userId;
    private String phone;
    private String name;
    private String role;
    private Integer memberLevel;
    private Integer points;
}
