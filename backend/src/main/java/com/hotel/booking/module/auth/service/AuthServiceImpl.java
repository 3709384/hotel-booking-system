package com.hotel.booking.module.auth.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hotel.booking.common.BusinessException;
import com.hotel.booking.common.ResultCode;
import com.hotel.booking.module.auth.dto.LoginReq;
import com.hotel.booking.module.auth.dto.LoginResp;
import com.hotel.booking.module.auth.dto.RegisterReq;
import com.hotel.booking.module.auth.dto.UserVO;
import com.hotel.booking.module.user.entity.User;
import com.hotel.booking.module.user.mapper.UserMapper;
import com.hotel.booking.module.user.util.PasswordUtil;
import com.hotel.booking.security.JwtUtil;
import com.hotel.booking.security.LoginUser;
import com.hotel.booking.security.UserContext;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(UserMapper userMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Long register(RegisterReq req) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (exists != null && exists > 0) {
            throw new BusinessException(ResultCode.PHONE_EXISTS);
        }
        String salt = PasswordUtil.generateSalt();
        User user = new User();
        user.setPhone(req.getPhone());
        user.setSalt(salt);
        user.setPasswordHash(PasswordUtil.hash(req.getPassword(), salt));
        user.setName(req.getName());
        user.setGender(0);
        user.setRole("MEMBER");
        user.setMemberLevel(1);
        user.setPoints(0);
        user.setStatus(1);
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    public LoginResp login(LoginReq req) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getPhone, req.getPhone()));
        if (user == null || !PasswordUtil.matches(req.getPassword(), user.getSalt(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.BAD_CREDENTIALS);
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.ACCOUNT_DISABLED);
        }
        LoginUser loginUser = new LoginUser(user.getId(), user.getRole(), user.getName(), user.getPhone());
        String token = jwtUtil.generate(loginUser);
        return new LoginResp(token, user.getId(), user.getName(), user.getRole(), jwtUtil.getExpireSeconds());
    }

    @Override
    public UserVO currentUser() {
        Long userId = UserContext.currentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        UserVO vo = new UserVO();
        vo.setUserId(user.getId());
        vo.setPhone(user.getPhone());
        vo.setName(user.getName());
        vo.setRole(user.getRole());
        vo.setMemberLevel(user.getMemberLevel());
        vo.setPoints(user.getPoints());
        return vo;
    }
}
