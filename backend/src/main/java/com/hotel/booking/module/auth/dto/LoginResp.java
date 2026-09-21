package com.hotel.booking.module.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResp {

    private String token;
    private Long userId;
    private String name;
    private String role;
    private long expiresIn;
}
