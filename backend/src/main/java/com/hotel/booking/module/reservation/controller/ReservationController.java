package com.hotel.booking.module.reservation.controller;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.Result;
import com.hotel.booking.module.reservation.dto.CreateReservationReq;
import com.hotel.booking.module.reservation.dto.ReservationVO;
import com.hotel.booking.module.reservation.service.ReservationService;
import com.hotel.booking.security.RequireRole;
import com.hotel.booking.security.UserContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @RequireRole("MEMBER")
    public Result<ReservationVO> create(@Valid @RequestBody CreateReservationReq req) {
        return Result.ok(reservationService.create(req, UserContext.currentUserId()));
    }

    @GetMapping("/{orderNo}")
    public Result<ReservationVO> detail(@PathVariable String orderNo) {
        return Result.ok(reservationService.detail(orderNo, UserContext.get()));
    }

    @GetMapping("/search")
    @RequireRole({"RECEPTIONIST", "ADMIN"})
    public Result<PageResult<ReservationVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate checkInDate,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size) {
        return Result.ok(reservationService.search(keyword, status, checkInDate, page, size));
    }

    @GetMapping("/mine")
    @RequireRole("MEMBER")
    public Result<PageResult<ReservationVO>> mine(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reservationService.mine(UserContext.currentUserId(), status, page, size));
    }

    @PostMapping("/{orderNo}/cancel")
    @RequireRole("MEMBER")
    public Result<ReservationVO> cancel(@PathVariable String orderNo) {
        return Result.ok(reservationService.cancel(orderNo, UserContext.currentUserId()));
    }
}
