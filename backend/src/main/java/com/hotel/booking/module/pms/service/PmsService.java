package com.hotel.booking.module.pms.service;

import com.hotel.booking.module.pms.dto.CheckInReq;
import com.hotel.booking.module.pms.dto.RoomBoardVO;
import com.hotel.booking.module.reservation.dto.ReservationVO;

import java.util.List;

public interface PmsService {

    /** 房态盘（可按楼层筛选）。 */
    List<RoomBoardVO> board(Integer floor);

    /** 排房并办理入住。 */
    ReservationVO checkIn(String orderNo, CheckInReq req);

    /** 办理退房，关联房间转待清洁。 */
    ReservationVO checkOut(String orderNo);

    /** 修改房态（清洁完成、维修等）。 */
    void updateRoomStatus(Long roomId, String status, String remark);
}
