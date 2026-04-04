package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.entity.User;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.utils.JwtUtil;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void register(RegisterDTO request) {
        // 1. 判重
        User existUser = userMapper.selectByUsername(request.getUsername());
        if (existUser != null) {
            throw new BusinessException(400, "用户名已存在");
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
            throw new BusinessException(500, "注册失败");
        }
    }

    @Override
    public LoginVO login(LoginUser user) {
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
        return loginVO;
    }

    @Override
    public UserInfoVO getUserInfo(LoginUser loginUser) {
        UserInfoVO userInfoVO = new UserInfoVO();
        userInfoVO.setId(loginUser.getId());
        userInfoVO.setUsername(loginUser.getUsername());
        userInfoVO.setNickname(loginUser.getNickname());
        userInfoVO.setEmail(loginUser.getEmail());
        userInfoVO.setRole(loginUser.getRole());
        return userInfoVO;
    }

}
