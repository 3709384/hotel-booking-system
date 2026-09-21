package com.hotel.booking.module.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户实体（会员与员工，role 区分）。对应表 user。
 */
@Data
@TableName("`user`")
public class User {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;
    private String passwordHash;
    private String salt;
    private String name;
    private String idCard;
    private Integer gender;
    /** MEMBER / RECEPTIONIST / ADMIN */
    private String role;
    private Integer memberLevel;
    private Integer points;
    /** 1 正常 0 停用 */
    private Integer status;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
