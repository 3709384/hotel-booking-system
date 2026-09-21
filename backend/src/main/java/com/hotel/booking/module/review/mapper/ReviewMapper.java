package com.hotel.booking.module.review.mapper;

import com.hotel.booking.module.review.dto.ReviewSummaryVO;
import com.hotel.booking.module.review.dto.ReviewVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReviewMapper {

    @Select("""
            SELECT r.id, u.name AS user_name, r.score, r.content, r.created_at
            FROM review r
            JOIN user u ON u.id = r.user_id
            WHERE r.room_type_id = #{roomTypeId} AND r.status = 1
            ORDER BY r.created_at DESC, r.id DESC
            LIMIT #{offset}, #{size}
            """)
    List<ReviewVO> selectPage(@Param("roomTypeId") Long roomTypeId,
                              @Param("offset") long offset,
                              @Param("size") long size);

    @Select("SELECT COUNT(*) FROM review WHERE room_type_id = #{roomTypeId} AND status = 1")
    long countByRoomType(@Param("roomTypeId") Long roomTypeId);

    @Select("""
            SELECT COALESCE(AVG(score), 0) AS average_score,
                   COUNT(*) AS review_count
            FROM review
            WHERE room_type_id = #{roomTypeId} AND status = 1
            """)
    ReviewSummaryVO summaryByRoomType(@Param("roomTypeId") Long roomTypeId);
}
