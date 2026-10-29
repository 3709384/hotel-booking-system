package com.hotel.booking.module.hotel.controller;

import com.hotel.booking.common.Result;
import com.hotel.booking.module.hotel.dto.HotelVO;
import com.hotel.booking.module.hotel.service.HotelService;
import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * C 端酒店平台接口（多酒店模型）：列表支持关键词/排序/标签快筛，详情含房型。
 * 与房型浏览一致，免登录可访问（见 WebMvcConfig 放行 /hotels/**）。
 */
@RestController
public class HotelController {

    private final HotelService hotelService;
    private final ReviewService reviewService;

    public HotelController(HotelService hotelService, ReviewService reviewService) {
        this.hotelService = hotelService;
        this.reviewService = reviewService;
    }

    @GetMapping("/hotels")
    public Result<List<HotelVO>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String tag) {
        return Result.ok(hotelService.listHotels(keyword, sort, tag));
    }

    @GetMapping("/hotels/{id}")
    public Result<HotelVO> detail(@PathVariable Long id) {
        HotelVO hotel = hotelService.getHotel(id);
        if (hotel.getRoomTypes() != null) {
            hotel.getRoomTypes().forEach(type -> {
                ReviewSummaryVO summary = reviewService.summary(type.getId());
                type.setAverageScore(summary.getAverageScore());
                type.setReviewCount(summary.getReviewCount());
            });
        }
        return Result.ok(hotel);
    }
}
