package org.campus.classroom.controller;

import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.RegisterRequest;
import org.campus.classroom.entity.User;
import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.utils.JWTUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserMapper userMapper;
    private  final AuthService authService;
    private final JWTUtils jwtUtils;

    @PostMapping("/login")
    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) {
            // 登录成功，重定向到主页
            String token=jwtUtils.generateToken(user.getId(), user.getUsername(), user.getRole(), 3600000);
            return token;
        }
        return "fail";
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterRequest request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @GetMapping("/me")
    public String me() {
        return "hhh";
    }
}
