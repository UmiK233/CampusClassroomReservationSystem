package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.LoginDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Valid LoginDTO request) {
        String username = request.getUsername();
        String password = request.getPassword();
        //通过SpringSecurity对账号密码进行认证
        try {
            //① 查用户 UserDetailsService
            //② 校验密码 PasswordEncoder
            //③ 状态校验 UserDetails 的属性
            UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(usernamePasswordAuthenticationToken);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            LoginVO loginVO = authService.login(loginUser);
            return Result.success("登录成功", loginVO);
        } catch (InternalAuthenticationServiceException e) {
//            Throwable cause = e.getCause();
//            log.error("真实异常: ", cause);
            throw new BusinessException(ResultCode.FORBIDDEN, "用户已被封禁");
        } catch (AuthenticationException e) {
            // 兜底异常，防止其他未知的认证异常导致系统崩溃
            log.info("[登录失败] username={}, reason=authentication failed, exception={}", username, e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
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
