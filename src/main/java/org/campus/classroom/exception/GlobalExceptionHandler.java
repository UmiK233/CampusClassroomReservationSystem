package org.campus.classroom.exception;


import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.campus.classroom.common.Result;
import org.campus.classroom.enums.ResultCode;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.format.DateTimeParseException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // 处理：不支持的媒体类型（前端传了form-data，接口要求json）
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public Result<String> handleHttpMediaTypeNotSupportedException(HttpMediaTypeNotSupportedException e) {
        log.error("媒体类型错误: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, "请使用 JSON 格式请求");
    }

    // 处理：不支持的请求类型
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Result<String> handleHttpRequestMethodNotSupportedException(HttpRequestMethodNotSupportedException e) {
        log.error("请求类型错误: {}", e.getMessage());
        return Result.fail(ResultCode.BAD_REQUEST, e.getMessage());
    }

    // 处理：不支持的请求类型
    @ExceptionHandler(NoResourceFoundException.class)
    public Result<String> handleNoResourceFoundException(NoResourceFoundException e) {
        log.error("请求的资源不存在: {}", e.getMessage());
        return Result.fail(ResultCode.INTERNAL_ERROR, "请求的资源不存在");
    }

//    // 401 未登录
//    @ExceptionHandler(AuthenticationException.class)
//    public Result<String> handleAuthenticationException(AuthenticationException e) {
//        return Result.fail(ResultCode.UNAUTHORIZED, "未登录或Token无效");
//    }

    // 403 权限不足
    @ExceptionHandler(AuthorizationDeniedException.class)
    public Result<String> handleAuthorizationDeniedException(AuthorizationDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN, "无权限访问");
    }

    // 捕获 JWT 过期异常
    @ExceptionHandler(ExpiredJwtException.class)
    public Result<String> handleExpiredJwtException(ExpiredJwtException e) {
        // 返回项目统一的格式就行
        return Result.fail(ResultCode.UNAUTHORIZED, "登录状态已过期，请重新登录");
    }

    @ExceptionHandler(JwtException.class)
    public Result<String> handleJwtException(JwtException e) {
        return Result.fail(ResultCode.UNAUTHORIZED, "无效的登录状态，请重新登录");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public Result<String> handleBadCredentialsException(BadCredentialsException e) {
        return Result.fail(ResultCode.UNAUTHORIZED, "认证失败，请检查用户名和密码");
    }

    @ExceptionHandler(AccessDeniedException.class)
    public Result<String> handleAccessDeniedException(AccessDeniedException e) {
        return Result.fail(ResultCode.FORBIDDEN, "权限不足，无法访问");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<String> handleJsonParseError(HttpMessageNotReadableException e) {

        Throwable cause = e.getCause();

        // 1. 如果是 JSON 格式错误（时间、数字、枚举等）
        if (cause instanceof InvalidFormatException) {
            // 2. 判断是不是时间解析异常
            if (cause.getCause() instanceof DateTimeParseException ||
                    cause.getCause() instanceof java.time.DateTimeException) {
                return Result.fail(ResultCode.BAD_REQUEST, "时间格式错误或时间值非法");
            }

            // 其他类型错误（如字符串转数字）
            return Result.fail(ResultCode.BAD_REQUEST, "参数类型不正确");
        }

        // 3. 其他 JSON 错误（格式非法、少逗号等）
        return Result.fail(ResultCode.BAD_REQUEST, "请求参数格式不正确");
    }


    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getResultCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        // 从异常中获取所有校验失败的字段信息
        String message = e.getBindingResult()
                .getFieldErrors() // 拿到所有校验失败的字段（比如status）
                .stream()
                .findFirst() // 只取第一个错误
                .map(error -> error.getField() + " " + error.getDefaultMessage()) // 拼接提示：字段名 + 错误信息（比如 "status 不能为空"）
                .orElse("参数校验失败"); // 如果没拿到错误信息，默认提示

        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        // 1. 从异常中获取校验失败信息
        String message = e.getConstraintViolations() // 拿到所有路径/查询参数的校验失败信息
                .stream()
                .findFirst() // 只取第一个错误
                .map(ConstraintViolation::getMessage) // 拿到错误提示（比如 "id 不能小于1"）
                .orElse("参数校验失败"); // 默认提示

        return Result.fail(ResultCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error(e.getMessage(), e);
        return Result.fail(ResultCode.INTERNAL_ERROR, "系统内部错误");
    }
}