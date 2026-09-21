package com.hotel.booking.module.room.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hotel.booking.common.PageResult;
import com.hotel.booking.common.Result;
import com.hotel.booking.module.room.entity.Room;
import com.hotel.booking.module.room.entity.RoomType;
import com.hotel.booking.module.room.mapper.RoomMapper;
import com.hotel.booking.module.room.mapper.RoomTypeMapper;
import com.hotel.booking.module.room.service.RoomService;
import com.hotel.booking.security.RequireRole;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 后台资源管理（ADMIN）。新增/修改房型、新增房间、初始化库存。
 */
@RestController
@RequestMapping("/admin")
@RequireRole("ADMIN")
public class RoomAdminController {

    private final RoomTypeMapper roomTypeMapper;
    private final RoomMapper roomMapper;
    private final RoomService roomService;

    public RoomAdminController(RoomTypeMapper roomTypeMapper, RoomMapper roomMapper,
                               RoomService roomService) {
        this.roomTypeMapper = roomTypeMapper;
        this.roomMapper = roomMapper;
        this.roomService = roomService;
    }

    @GetMapping("/room-types")
    public Result<PageResult<RoomType>> listTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<RoomType> wrapper = new LambdaQueryWrapper<RoomType>()
                .and(hasKeyword, w -> w.like(RoomType::getName, keyword).or().like(RoomType::getCode, keyword))
                .orderByAsc(RoomType::getSortOrder)
                .orderByAsc(RoomType::getId);
        Page<RoomType> result = roomTypeMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @GetMapping("/rooms")
    public Result<PageResult<Room>> listRooms(
            @RequestParam(required = false) Long roomTypeId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "10") long size) {
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        LambdaQueryWrapper<Room> wrapper = new LambdaQueryWrapper<Room>()
                .eq(roomTypeId != null, Room::getRoomTypeId, roomTypeId)
                .eq(status != null && !status.isBlank(), Room::getStatus, status)
                .like(hasKeyword, Room::getRoomNo, keyword)
                .orderByAsc(Room::getFloor)
                .orderByAsc(Room::getRoomNo);
        Page<Room> result = roomMapper.selectPage(new Page<>(page, size), wrapper);
        return Result.ok(PageResult.of(result.getRecords(), result.getTotal(), page, size));
    }

    @PostMapping("/room-types")
    public Result<Map<String, Object>> createType(@RequestBody RoomType roomType) {
        if (roomType.getStatus() == null) {
            roomType.setStatus(1);
        }
        roomTypeMapper.insert(roomType);
        return Result.ok(Map.of("id", roomType.getId()));
    }

    @PutMapping("/room-types/{id}")
    public Result<Void> updateType(@PathVariable Long id, @RequestBody RoomType roomType) {
        roomType.setId(id);
        roomTypeMapper.updateById(roomType);
        return Result.ok();
    }

    @PostMapping("/rooms")
    public Result<Map<String, Object>> createRoom(@RequestBody Room room) {
        if (room.getStatus() == null) {
            room.setStatus("VC");
        }
        roomMapper.insert(room);
        return Result.ok(Map.of("id", room.getId()));
    }

    @PostMapping("/inventory/init")
    public Result<Map<String, Object>> initInventory(@RequestParam(defaultValue = "90") int days) {
        int created = roomService.initInventory(days);
        return Result.ok(Map.of("createdRows", created));
    }
}
