package org.campus.classroom.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.LoginDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO request) {
        String username=request.getUsername();
        String password=request.getPassword();
        //通过SpringSecurity对账号密码进行认证
        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
        Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        LoginVO loginVO = authService.login(loginUser);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getUserInfo(@AuthenticationPrincipal LoginUser user) {
        UserInfoVO userInfoVO = authService.getUserInfo(user);
        return Result.success("获取成功", userInfoVO);
    }
}
