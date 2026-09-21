package com.hotel.booking.module.auth.service;

import com.hotel.booking.module.auth.dto.LoginReq;
import com.hotel.booking.module.auth.dto.LoginResp;
import com.hotel.booking.module.auth.dto.RegisterReq;
import com.hotel.booking.module.auth.dto.UserVO;

public interface AuthService {

    /** 会员注册，返回新用户ID。 */
    Long register(RegisterReq req);

    /** 登录，返回访问令牌。 */
    LoginResp login(LoginReq req);

    /** 获取当前登录用户信息。 */
    UserVO currentUser();
}
