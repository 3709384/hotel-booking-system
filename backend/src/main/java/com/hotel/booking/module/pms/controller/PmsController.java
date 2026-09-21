package com.hotel.booking.module.pms.controller;

import com.hotel.booking.common.Result;
import com.hotel.booking.module.pms.dto.CheckInReq;
import com.hotel.booking.module.pms.dto.RoomBoardVO;
import com.hotel.booking.module.pms.dto.UpdateRoomStatusReq;
import com.hotel.booking.module.pms.service.PmsService;
import com.hotel.booking.module.reservation.dto.ReservationVO;
import com.hotel.booking.security.RequireRole;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 前台接待（PMS）。RECEPTIONIST 与 ADMIN 可访问。
 */
@RestController
@RequestMapping("/pms")
@RequireRole({"RECEPTIONIST", "ADMIN"})
public class PmsController {

    private final PmsService pmsService;

    public PmsController(PmsService pmsService) {
        this.pmsService = pmsService;
    }

    @GetMapping("/rooms/board")
    public Result<List<RoomBoardVO>> board(@RequestParam(required = false) Integer floor) {
        return Result.ok(pmsService.board(floor));
    }

    @PostMapping("/reservations/{orderNo}/check-in")
    public Result<ReservationVO> checkIn(@PathVariable String orderNo, @Valid @RequestBody CheckInReq req) {
        return Result.ok(pmsService.checkIn(orderNo, req));
    }

    @PostMapping("/reservations/{orderNo}/check-out")
    public Result<ReservationVO> checkOut(@PathVariable String orderNo) {
        return Result.ok(pmsService.checkOut(orderNo));
    }

    @PutMapping("/rooms/{id}/status")
    public Result<Void> updateRoomStatus(@PathVariable Long id, @Valid @RequestBody UpdateRoomStatusReq req) {
        pmsService.updateRoomStatus(id, req.getStatus(), req.getRemark());
        return Result.ok();
    }
}
