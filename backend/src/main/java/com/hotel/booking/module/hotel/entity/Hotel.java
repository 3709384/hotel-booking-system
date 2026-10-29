package com.hotel.booking.module.hotel.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 酒店。对应表 hotel（2026-10-29 多酒店平台改版新增）。
 * 房型通过 room_type.hotel_id 归属到酒店。
 */
@Data
@TableName(value = "hotel", autoResultMap = true)
public class Hotel {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
    /** 档次：经济型/舒适型/高档型 */
    private String kind;
    /** 展示评分（1-5，一位小数） */
    private BigDecimal score;
    private String scoreWord;
    /** 住客代表性短评（列表页展示） */
    private String quote;

    /** 酒店标签，JSON 数组 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> tags;

    private BigDecimal distanceKm;
    private String distanceText;
    private String landmark;
    /** 连续好评住客数（可空） */
    private Integer streak;
    private String salesText;
    private String badge;
    /** 促销类型：神券/会员 */
    private String promoType;
    private String promoText;
    /** 是否黄金会员价酒店：1 是 0 否 */
    private Integer member;
    private String coverImage;
    private String icon;

    /** 1 上架 0 下架 */
    private Integer status;
    private Integer sortOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
