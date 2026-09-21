package com.hotel.booking.module.room.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 物理房间。对应表 room。房态 status：VC 空闲可售 / VD 待清洁 / OC 在住 / OOO 维修停用。
 */
@Data
@TableName("room")
public class Room {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String roomNo;
    private Long roomTypeId;
    private Integer floor;
    private String status;
    private String remark;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
