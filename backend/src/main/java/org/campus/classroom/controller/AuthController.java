package org.campus.classroom.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.common.Result;
import org.campus.classroom.dto.ChangePasswordDTO;
import org.campus.classroom.dto.EmailCodeSendDTO;
import org.campus.classroom.dto.EmailLoginDTO;
import org.campus.classroom.dto.LoginDTO;
import org.campus.classroom.dto.RefreshTokenDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.dto.ResetPasswordByCodeDTO;
import org.campus.classroom.dto.UpdateNicknameDTO;
import org.campus.classroom.enums.ResultCode;
import org.campus.classroom.exception.BusinessException;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.service.AuthService;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        try {
            authService.validateLoginAccount(username);
            UsernamePasswordAuthenticationToken token =
                    new UsernamePasswordAuthenticationToken(username, password);
            Authentication authentication = authenticationManager.authenticate(token);
            LoginUser loginUser = (LoginUser) authentication.getPrincipal();
            LoginVO loginVO = authService.login(loginUser);
            return Result.success("登录成功", loginVO);
        } catch (InternalAuthenticationServiceException e) {
            if (e.getCause() instanceof BusinessException businessException) {
                throw businessException;
            }
            log.error("[登录失败] username={}, 原因=认证服务异常", username, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "登录失败，请稍后重试");
        } catch (AuthenticationException e) {
            log.info("[登录失败] username={}, 原因=密码错误或认证失败, message={}", username, e.getMessage());
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[登录失败] username={}, 原因=系统异常", username, e);
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "登录失败，请稍后重试");
        }
    }

    @PostMapping("/login/code")
    public Result<LoginVO> loginByEmailCode(@RequestBody @Valid EmailLoginDTO request) {
        LoginVO loginVO = authService.loginByEmailCode(request);
        return Result.success("登录成功", loginVO);
    }

    @PostMapping("/email-code")
    public Result<Void> sendEmailCode(@RequestBody @Valid EmailCodeSendDTO request) {
        authService.sendEmailCode(request);
        return Result.success("验证码发送成功");
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody @Valid RegisterDTO request) {
        authService.register(request);
        return Result.success("注册成功");
    }

    @PostMapping("/password/reset")
    public Result<Void> resetPasswordByEmailCode(@RequestBody @Valid ResetPasswordByCodeDTO request) {
        authService.resetPasswordByEmailCode(request);
        return Result.success("重置密码成功");
    }

    @GetMapping("/me")
    public Result<UserInfoVO> getUserInfo(@AuthenticationPrincipal LoginUser user) {
        UserInfoVO userInfoVO = authService.getUserInfo(user);
        return Result.success("获取成功", userInfoVO);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@RequestBody @Valid ChangePasswordDTO request,
                                       @AuthenticationPrincipal LoginUser user) {
        authService.changePassword(user.getId(), request);
        return Result.success("修改密码成功");
    }

    @PutMapping("/nickname")
    public Result<UserInfoVO> updateNickname(@RequestBody @Valid UpdateNicknameDTO request,
                                             @AuthenticationPrincipal LoginUser user) {
        UserInfoVO userInfoVO = authService.updateNickname(user.getId(), request);
        return Result.success("修改昵称成功", userInfoVO);
    }

    @PostMapping("/refresh")
    public Result<LoginVO> refresh(@RequestBody @Valid RefreshTokenDTO request) {
        LoginVO loginVO = authService.refresh(request.getRefreshToken());
        return Result.success("刷新成功", loginVO);
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestBody @Valid RefreshTokenDTO request) {
        authService.logout(request.getRefreshToken());
        return Result.success("退出成功");
    }
}
