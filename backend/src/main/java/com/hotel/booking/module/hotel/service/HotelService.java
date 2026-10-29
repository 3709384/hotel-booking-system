package com.hotel.booking.module.hotel.service;

import com.hotel.booking.module.hotel.dto.HotelVO;

import java.util.List;

public interface HotelService {

    /**
     * 酒店列表（平台首页）。
     *
     * @param keyword 关键词：匹配酒店名 / 地标 / 标签
     * @param sort    排序：smart 智能(默认) / priceAsc / priceDesc / score / distance
     * @param tag     快筛标签：神券商家 / 黄金会员 / 大床房 / 双床房 / 其余按酒店标签匹配
     */
    List<HotelVO> listHotels(String keyword, String sort, String tag);

    /** 酒店详情（含其在售房型，房型点评汇总由 Controller 侧 enrich）。 */
    HotelVO getHotel(Long id);
}
