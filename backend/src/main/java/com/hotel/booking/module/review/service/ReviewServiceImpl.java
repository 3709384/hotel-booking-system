package com.hotel.booking.module.review.service;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.dto.ReviewVO;
import com.hotel.booking.module.review.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    @Override
    public PageResult<ReviewVO> listByRoomType(Long roomTypeId, long page, long size) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        long offset = (safePage - 1) * safeSize;
        List<ReviewVO> records = reviewMapper.selectPage(roomTypeId, offset, safeSize);
        long total = reviewMapper.countByRoomType(roomTypeId);
        return PageResult.of(records, total, safePage, safeSize);
    }

    @Override
    public ReviewSummaryVO summary(Long roomTypeId) {
        ReviewSummaryVO summary = reviewMapper.summaryByRoomType(roomTypeId);
        if (summary == null) {
            summary = new ReviewSummaryVO();
        }
        if (summary.getAverageScore() == null) {
            summary.setAverageScore(BigDecimal.ZERO);
        } else {
            summary.setAverageScore(summary.getAverageScore().setScale(1, RoundingMode.HALF_UP));
        }
        if (summary.getReviewCount() == null) {
            summary.setReviewCount(0L);
        }
        return summary;
    }
}
