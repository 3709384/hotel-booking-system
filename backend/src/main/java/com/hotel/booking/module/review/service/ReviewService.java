package com.hotel.booking.module.review.service;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.dto.ReviewVO;

public interface ReviewService {
    PageResult<ReviewVO> listByRoomType(Long roomTypeId, long page, long size);

    ReviewSummaryVO summary(Long roomTypeId);
}
