package com.hotel.booking.module.room.entity;

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
 * 客房类型。对应表 room_type。
 */
@Data
@TableName(value = "room_type", autoResultMap = true)
public class RoomType {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String code;
    private String name;
    private String bedType;
    private Integer maxOccupancy;
    private BigDecimal area;
    /** 基础房价（元/间夜），MVP 按此计价 */
    private BigDecimal basePrice;
    private String description;
    private String coverImage;

    /** 设施清单，JSON 数组，借助 JacksonTypeHandler 与 List 互转 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> amenities;

    /** 1 上架 0 停用 */
    private Integer status;
    private Integer sortOrder;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
