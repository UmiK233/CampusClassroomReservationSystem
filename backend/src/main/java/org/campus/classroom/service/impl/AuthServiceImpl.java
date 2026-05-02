package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.dto.ChangePasswordDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.entity.User;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.service.SystemConfigService;
import org.campus.classroom.service.TokenService;
import org.campus.classroom.utils.JwtUtil;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.data.util.Pair;
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
    private final TokenService tokenService;

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
    public void validateLoginAccount(String username) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.FORBIDDEN, "用户已被封禁");
        }
    }

    @Override
    public LoginVO login(LoginUser loginUser) {
        log.info("[开始登录] 用户ID={}, 用户名={}", loginUser.getId(), loginUser.getUsername());
        String accessToken = jwtUtil.generateToken(loginUser);
        String refreshToken = tokenService.generateRefreshToken(loginUser.getId());
        LoginVO loginVO = buildLoginVO(loginUser, accessToken, refreshToken);
        log.info("[登录成功] 用户ID={}, 用户名={}, 角色={}", loginUser.getId(), loginUser.getUsername(), loginUser.getRole());
        return loginVO;
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        log.info("[刷新 Token] request received");
        Pair<Long, String> rotateResult = tokenService.rotateRefreshToken(refreshToken);
        LoginUser loginUser = new LoginUser(requireActiveUser(rotateResult.getFirst()));
        String accessToken = jwtUtil.generateToken(loginUser);
        return buildLoginVO(loginUser, accessToken, rotateResult.getSecond());
    }

    @Override
    public void logout(String refreshToken) {
        tokenService.revokeRefreshToken(refreshToken);
    }

    @Override
    public UserInfoVO getUserInfo(LoginUser loginUser) {
        log.info("[获取用户信息] 用户ID={}, 用户名={}", loginUser.getId(), loginUser.getUsername());
        return buildUserInfoVO(loginUser);
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO request) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "旧密码错误");
        }
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "新密码不能与旧密码相同");
        }

        int rows = userMapper.updatePassword(userId, passwordEncoder.encode(request.getNewPassword()));
        if (rows != 1) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "修改密码失败");
        }
        int updated = userMapper.incrementTokenVersion(userId);
        if (updated != 1) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "修改密码后未能刷新登录状态");
        }
        tokenService.revokeAllRefreshTokens(userId);
        log.info("[修改密码成功] 用户ID={}, 用户名={}", user.getId(), user.getUsername());
    }

    private User requireActiveUser(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户状态异常");
        }
        return user;
    }

    private LoginVO buildLoginVO(LoginUser loginUser, String accessToken, String refreshToken) {
        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserInfo(buildUserInfoVO(loginUser));
        return loginVO;
    }

    private UserInfoVO buildUserInfoVO(LoginUser loginUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(loginUser.getId());
        userInfoVO.setUsername(loginUser.getUsername());
        userInfoVO.setNickname(loginUser.getNickname());
        userInfoVO.setEmail(loginUser.getEmail());
        userInfoVO.setRole(loginUser.getRole());
        fillUserCreditInfo(userInfoVO, loginUser.getRole(), loginUser.getCreditScore());
        return userInfoVO;
    }

    private void fillUserCreditInfo(UserInfoVO userInfoVO, String role, Integer creditScore) {
        Integer quotaCreditScore = resolveQuotaCreditScore(role, creditScore);
        userInfoVO.setCreditScore(quotaCreditScore);
        userInfoVO.setCreditLevel(systemConfigService.getCreditLevelCode(quotaCreditScore));
        userInfoVO.setSeatReservationAdvanceHours(systemConfigService.getSeatReservationAdvanceHours(quotaCreditScore));
        userInfoVO.setMaxSingleReservationMinutes(systemConfigService.getMaxSingleReservationMinutes(quotaCreditScore));
        userInfoVO.setDailyReservationLimitMinutes(systemConfigService.getDailyReservationLimitMinutes(quotaCreditScore));
    }

    private Integer resolveQuotaCreditScore(String role, Integer creditScore) {
        if ("TEACHER".equals(role)) {
            return systemConfigService.getCreditMaxScore();
        }
        return creditScore == null ? systemConfigService.getCreditMaxScore() : creditScore;
    }
}
