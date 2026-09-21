package com.hotel.booking.module.review.controller;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.Result;
import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.dto.ReviewVO;
import com.hotel.booking.module.review.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/room-types/{roomTypeId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public Result<PageResult<ReviewVO>> list(@PathVariable Long roomTypeId,
                                             @RequestParam(defaultValue = "1") long page,
                                             @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listByRoomType(roomTypeId, page, size));
    }

    @GetMapping("/summary")
    public Result<ReviewSummaryVO> summary(@PathVariable Long roomTypeId) {
        return Result.ok(reviewService.summary(roomTypeId));
    }
}
