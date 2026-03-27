package org.campus.classroom.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.LoginDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.security.JwtTokenProvider;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO request) {
        LoginVO loginVO = authService.login(request);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody RegisterDTO request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getUserInfo(HttpServletRequest httpRequest) {
        String authHeader=httpRequest.getHeader("Authorization");
        UserInfoVO userInfoVO = authService.getUserInfo(authHeader);
        return Result.success("获取成功", userInfoVO);
    }
}
