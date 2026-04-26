package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.entity.User;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.service.SystemConfigService;
import org.campus.classroom.utils.JwtUtil;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;
    private final SystemConfigService systemConfigService;

    @Override
    public void register(RegisterDTO request) {
        log.info("[开始注册] 用户名={}, 邮箱={}", request.getUsername(), request.getEmail());
        User existUser = userMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            log.warn("[注册失败] 用户名={}, 原因=用户名重复", request.getUsername());
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("STUDENT");

        int rows = userMapper.insert(user);
        if (rows <= 0) {
            log.error("[注册失败] 用户名={}, 原因=写入数据库失败", request.getUsername());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "注册失败");
        }
        log.info("[注册成功] 用户ID={}, 用户名={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginUser user) {
        log.info("[开始登录] 用户ID={}, 用户名={}", user.getId(), user.getUsername());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setEmail(user.getEmail());
        userInfoVO.setRole(user.getRole());
        fillUserCreditInfo(userInfoVO, user.getCreditScore());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userInfoVO);
        log.info("[登录成功] 用户ID={}, 用户名={}, 角色={}", user.getId(), user.getUsername(), user.getRole());
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(LoginUser loginUser) {
        log.info("[获取用户信息] 用户ID={}, 用户名={}", loginUser.getId(), loginUser.getUsername());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(loginUser.getId());
        userInfoVO.setUsername(loginUser.getUsername());
        userInfoVO.setNickname(loginUser.getNickname());
        userInfoVO.setEmail(loginUser.getEmail());
        userInfoVO.setRole(loginUser.getRole());
        fillUserCreditInfo(userInfoVO, loginUser.getCreditScore());
        return userInfoVO;
    }

    private void fillUserCreditInfo(UserInfoVO userInfoVO, Integer creditScore) {
        userInfoVO.setCreditScore(creditScore == null ? systemConfigService.getCreditMaxScore() : creditScore);
        userInfoVO.setCreditLevel(systemConfigService.getCreditLevelCode(creditScore));
        userInfoVO.setSeatReservationAdvanceHours(systemConfigService.getSeatReservationAdvanceHours(creditScore));
        userInfoVO.setMaxSingleReservationMinutes(systemConfigService.getMaxSingleReservationMinutes(creditScore));
        userInfoVO.setDailyReservationLimitMinutes(systemConfigService.getDailyReservationLimitMinutes(creditScore));
    }
}
