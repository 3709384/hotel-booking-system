package com.hotel.booking.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 房型每日库存（按"夜"售卖的核心）。对应表 room_inventory。
 * 可售间数 = totalCount - bookedCount。
 */
@Data
@TableName("room_inventory")
public class RoomInventory {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long roomTypeId;
    private LocalDate inventoryDate;
    private Integer totalCount;
    private Integer bookedCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
