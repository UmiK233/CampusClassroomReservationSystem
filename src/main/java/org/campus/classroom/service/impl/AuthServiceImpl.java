package org.campus.classroom.service.impl;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.dto.RegisterRequest;
import org.campus.classroom.entity.User;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.service.AuthService;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserMapper userMapper;

    @Override
    public void register(RegisterRequest request) {
        // 1. 判重
        User existUser = userMapper.findByUsername(request.getUsername());
        if (existUser != null) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 组装用户对象
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(request.getPassword());
        // 默认昵称使用用户名
        user.setNickname(request.getUsername());
        user.setEmail(request.getEmail());
        user.setRole("STUDENT");

        // 3. 保存
        int rows = userMapper.insert(user);
        if (rows <= 0) {
            throw new RuntimeException("注册失败");
        }
    }
}
