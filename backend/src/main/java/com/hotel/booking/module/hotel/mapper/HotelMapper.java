package com.hotel.booking.module.hotel.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hotel.booking.module.hotel.entity.Hotel;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface HotelMapper extends BaseMapper<Hotel> {
}
