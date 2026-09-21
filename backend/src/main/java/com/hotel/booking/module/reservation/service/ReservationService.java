package com.hotel.booking.module.reservation.service;

import com.hotel.booking.common.PageResult;
import com.hotel.booking.module.reservation.dto.CreateReservationReq;
import com.hotel.booking.module.reservation.dto.ReservationVO;
import com.hotel.booking.module.reservation.entity.Reservation;
import com.hotel.booking.security.LoginUser;

public interface ReservationService {

    /** 创建订单：校验库存 → 事务内逐夜扣减 → 生成订单与明细。 */
    ReservationVO create(CreateReservationReq req, Long userId);

    /** 订单详情；会员仅能查本人订单，员工可查全部。 */
    ReservationVO detail(String orderNo, LoginUser current);

    /** 我的订单（分页）。 */
    PageResult<ReservationVO> mine(Long userId, String status, long page, long size);

    /** 会员取消订单（含库存回滚、免费取消时限校验）。 */
    ReservationVO cancel(String orderNo, Long userId);

    /** 支付成功确认（由支付回调调用），幂等。 */
    void confirmPaid(Long reservationId);

    /** 扫描并取消超时未支付订单，返回处理条数（定时任务调用）。 */
    int cancelExpired();

    /** 按订单号取订单，不存在抛异常。 */
    Reservation requireByOrderNo(String orderNo);
}
