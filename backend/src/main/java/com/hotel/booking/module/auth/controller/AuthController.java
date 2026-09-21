package com.hotel.booking.module.auth.controller;

import com.hotel.booking.common.Result;
import com.hotel.booking.module.auth.dto.LoginReq;
import com.hotel.booking.module.auth.dto.LoginResp;
import com.hotel.booking.module.auth.dto.RegisterReq;
import com.hotel.booking.module.auth.dto.UserVO;
import com.hotel.booking.module.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public Result<Map<String, Object>> register(@Valid @RequestBody RegisterReq req) {
        Long userId = authService.register(req);
        return Result.ok(Map.of("userId", userId, "phone", req.getPhone(), "role", "MEMBER"));
    }

    @PostMapping("/login")
    public Result<LoginResp> login(@Valid @RequestBody LoginReq req) {
        return Result.ok(authService.login(req));
    }

    @GetMapping("/me")
    public Result<UserVO> me() {
        return Result.ok(authService.currentUser());
    }
}
