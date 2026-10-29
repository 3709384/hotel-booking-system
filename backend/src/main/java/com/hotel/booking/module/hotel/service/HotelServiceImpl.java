package com.hotel.booking.module.hotel.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.module.hotel.dto.HotelVO;
import com.hotel.booking.module.hotel.entity.Hotel;
import com.hotel.booking.module.hotel.mapper.HotelMapper;
import com.hotel.booking.module.review.mapper.ReviewMapper;
import com.hotel.booking.module.room.dto.RoomTypeVO;
import com.hotel.booking.module.room.entity.RoomType;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HotelServiceImpl implements HotelService {

    private final HotelMapper hotelMapper;
    private final RoomTypeMapper roomTypeMapper;
    private final ReviewMapper reviewMapper;

    public HotelServiceImpl(HotelMapper hotelMapper, RoomTypeMapper roomTypeMapper,
                            ReviewMapper reviewMapper) {
        this.hotelMapper = hotelMapper;
        this.roomTypeMapper = roomTypeMapper;
        this.reviewMapper = reviewMapper;
    }

    @Override
    public List<HotelVO> listHotels(String keyword, String sort, String tag) {
        List<Hotel> hotels = hotelMapper.selectList(new LambdaQueryWrapper<Hotel>()
                .eq(Hotel::getStatus, 1)
                .orderByAsc(Hotel::getSortOrder).orderByAsc(Hotel::getId));

        // 一次性取全部在售房型，按酒店分组，用于起价与房型名筛选
        Map<Long, List<RoomType>> typesByHotel = roomTypeMapper.selectList(
                        new LambdaQueryWrapper<RoomType>().eq(RoomType::getStatus, 1))
                .stream().collect(Collectors.groupingBy(RoomType::getHotelId));

        List<HotelVO> result = new ArrayList<>();
        for (Hotel h : hotels) {
            List<RoomType> types = typesByHotel.getOrDefault(h.getId(), List.of());
            if (types.isEmpty()) {
                continue;
            }
            if (hasText(keyword) && !matchesKeyword(h, keyword)) {
                continue;
            }
            if (hasText(tag) && !matchesTag(h, types, tag)) {
                continue;
            }
            result.add(toVO(h, types));
        }
        sort(result, sort);
        return result;
    }

    @Override
    public HotelVO getHotel(Long id) {
        Hotel hotel = hotelMapper.selectById(id);
        if (hotel == null || hotel.getStatus() == null || hotel.getStatus() != 1) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        List<RoomType> types = roomTypeMapper.selectList(new LambdaQueryWrapper<RoomType>()
                .eq(RoomType::getHotelId, id)
                .eq(RoomType::getStatus, 1)
                .orderByAsc(RoomType::getSortOrder).orderByAsc(RoomType::getId));
        HotelVO vo = toVO(hotel, types);
        vo.setRoomTypes(types.stream().map(this::toRoomTypeVO).toList());
        return vo;
    }

    private HotelVO toVO(Hotel h, List<RoomType> types) {
        HotelVO vo = new HotelVO();
        vo.setId(h.getId());
        vo.setName(h.getName());
        vo.setKind(h.getKind());
        vo.setScore(h.getScore());
        vo.setScoreWord(h.getScoreWord());
        vo.setQuote(h.getQuote());
        vo.setTags(h.getTags());
        vo.setDistanceKm(h.getDistanceKm());
        vo.setDistanceText(h.getDistanceText());
        vo.setLandmark(h.getLandmark());
        vo.setStreak(h.getStreak());
        vo.setSalesText(h.getSalesText());
        vo.setBadge(h.getBadge());
        vo.setPromoType(h.getPromoType());
        vo.setPromoText(h.getPromoText());
        vo.setMember(h.getMember());
        vo.setCoverImage(h.getCoverImage());
        vo.setIcon(h.getIcon());
        RoomType cheapest = types.stream()
                .min(Comparator.comparing(RoomType::getBasePrice))
                .orElse(null);
        if (cheapest != null) {
            vo.setFromPrice(cheapest.getBasePrice());
            vo.setMarketPrice(cheapest.getMarketPrice() != null
                    ? cheapest.getMarketPrice() : cheapest.getBasePrice());
        }
        vo.setReviewCount(reviewMapper.countByHotel(h.getId()));
        return vo;
    }

    private RoomTypeVO toRoomTypeVO(RoomType t) {
        RoomTypeVO vo = new RoomTypeVO();
        vo.setId(t.getId());
        vo.setHotelId(t.getHotelId());
        vo.setCode(t.getCode());
        vo.setName(t.getName());
        vo.setBedType(t.getBedType());
        vo.setMaxOccupancy(t.getMaxOccupancy());
        vo.setArea(t.getArea());
        vo.setBasePrice(t.getBasePrice());
        vo.setMarketPrice(t.getMarketPrice());
        vo.setDescription(t.getDescription());
        vo.setCoverImage(t.getCoverImage());
        vo.setAmenities(t.getAmenities());
        return vo;
    }

    private boolean matchesKeyword(Hotel h, String keyword) {
        return (h.getName() != null && h.getName().contains(keyword))
                || (h.getLandmark() != null && h.getLandmark().contains(keyword))
                || (h.getTags() != null && h.getTags().stream().anyMatch(t -> t.contains(keyword)));
    }

    private boolean matchesTag(Hotel h, List<RoomType> types, String tag) {
        return switch (tag) {
            case "神券商家" -> "神券".equals(h.getPromoType());
            case "黄金会员" -> h.getMember() != null && h.getMember() == 1;
            case "大床房" -> types.stream().anyMatch(t -> t.getName().contains("大床"));
            case "双床房" -> types.stream().anyMatch(t -> t.getName().contains("双床"));
            default -> h.getTags() != null && h.getTags().contains(tag);
        };
    }

    private void sort(List<HotelVO> list, String sort) {
        if (!hasText(sort)) {
            return;
        }
        switch (sort) {
            case "priceAsc" -> list.sort(Comparator.comparing(
                    HotelVO::getFromPrice, Comparator.nullsLast(BigDecimal::compareTo)));
            case "priceDesc" -> list.sort(Comparator.comparing(
                    HotelVO::getFromPrice, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
            case "score" -> list.sort(Comparator.comparing(
                    HotelVO::getScore, Comparator.nullsLast(BigDecimal::compareTo)).reversed());
            case "distance" -> list.sort(Comparator.comparing(
                    HotelVO::getDistanceKm, Comparator.nullsLast(BigDecimal::compareTo)));
            default -> { /* smart：保持 sort_order  curated 顺序 */ }
        }
    }

    private boolean hasText(String s) {
        return s != null && !s.trim().isEmpty();
    }
}
