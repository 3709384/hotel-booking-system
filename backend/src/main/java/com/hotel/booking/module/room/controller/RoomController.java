package com.hotel.booking.module.room.controller;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.Result;
import com.hotel.booking.module.room.dto.AvailabilityVO;
import com.hotel.booking.module.room.dto.RoomTypeVO;
import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.service.ReviewService;
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
    private final ReviewService reviewService;

    public RoomController(RoomService roomService, ReviewService reviewService) {
        this.roomService = roomService;
        this.reviewService = reviewService;
    }

    @GetMapping("/room-types")
    public Result<PageResult<RoomTypeVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        PageResult<RoomTypeVO> result = roomService.listTypes(keyword, page, size);
        result.getRecords().forEach(this::enrich);
        return Result.ok(result);
    }

    @GetMapping("/room-types/{id}")
    public Result<RoomTypeVO> detail(@PathVariable Long id) {
        RoomTypeVO type = roomService.getType(id);
        enrich(type);
        return Result.ok(type);
    }

    @GetMapping("/availability")
    public Result<List<AvailabilityVO>> availability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut) {
        List<AvailabilityVO> result = roomService.availability(checkIn, checkOut);
        result.forEach(this::enrich);
        return Result.ok(result);
    }
    private void enrich(RoomTypeVO type) {
        ReviewSummaryVO summary = reviewService.summary(type.getId());
        type.setAverageScore(summary.getAverageScore());
        type.setReviewCount(summary.getReviewCount());
    }

    private void enrich(AvailabilityVO room) {
        ReviewSummaryVO summary = reviewService.summary(room.getRoomTypeId());
        room.setAverageScore(summary.getAverageScore());
        room.setReviewCount(summary.getReviewCount());
    }
}