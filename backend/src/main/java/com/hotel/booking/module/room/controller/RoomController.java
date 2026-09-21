package com.hotel.booking.module.room.controller;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.Result;
import com.hotel.booking.module.room.dto.AvailabilityVO;
import com.hotel.booking.module.room.dto.RoomTypeVO;
import com.hotel.booking.module.room.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/room-types")
    public Result<PageResult<RoomTypeVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        return Result.ok(roomService.listTypes(keyword, page, size));
    }

    @GetMapping("/room-types/{id}")
    public Result<RoomTypeVO> detail(@PathVariable Long id) {
        return Result.ok(roomService.getType(id));
    }

    @GetMapping("/availability")
    public Result<List<AvailabilityVO>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        return Result.ok(roomService.availability(checkIn, checkOut));
    }
}
