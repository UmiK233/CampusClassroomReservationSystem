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

    @Override
    public void register(RegisterDTO request) {
        log.info("[注册开始] username={}, email={}", request.getUsername(), request.getEmail());
        // 1. 判重
        User existUser = userMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            log.warn("[注册失败] username={}, reason=duplicate username", request.getUsername());
            throw new BusinessException(ResultCode.BAD_REQUEST, "用户名已存在");
        }

        // 2. 组装用户对象
        User user = new User();
        //Mapper默认自增id,不需要设置id,置为null即可
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        // 默认昵称使用用户名
        user.setNickname(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("STUDENT");

        // 3. 保存
        int rows = userMapper.insert(user);
        if (rows <= 0) {
            log.error("[注册失败] username={}, reason=insert failed", request.getUsername());
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "注册失败");
        }
        log.info("[注册成功] userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public LoginVO login(LoginUser user) {
        log.info("[登录开始] userId={}, username={}", user.getId(), user.getUsername());
/*
//          已经被SpringSecurity接管
//          ① 查用户 UserDetailsService
//          ② 校验密码 PasswordEncoder
//          ③ 状态校验 UserDetails 的属性
//        // 1. 查用户
//        User user = userMapper.selectByUsername(request.getUsername());
//        if (user == null) {
//            throw new BusinessException(400, "用户不存在");
//        }
//        // 2. 判断状态
//        if (user.getStatus() == null || user.getStatus() != 1) {
//            throw new BusinessException(400, "账号已被禁用");
//        }
//        // 3. 判断密码
//        if (!Objects.equals(user.getPassword(), request.getPassword())) {
//            throw new BusinessException(401, "用户名或密码错误");
//        }
 */
        // 4. 生成 token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        // 5. 生成UserInfo
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(user.getId());
        userInfoVO.setUsername(user.getUsername());
        userInfoVO.setNickname(user.getNickname());
        userInfoVO.setEmail(user.getEmail());
        userInfoVO.setRole(user.getRole());
        // 6. 返回结果
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUserInfo(userInfoVO);
        log.info("[登录成功] userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(LoginUser loginUser) {
        log.info("[获取用户信息] userId={}, username={}", loginUser.getId(), loginUser.getUsername());
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(loginUser.getId());
        userInfoVO.setUsername(loginUser.getUsername());
        userInfoVO.setNickname(loginUser.getNickname());
        userInfoVO.setEmail(loginUser.getEmail());
        userInfoVO.setRole(loginUser.getRole());
        return userInfoVO;
    }

}
