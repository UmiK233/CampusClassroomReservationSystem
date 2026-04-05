package org.campus.classroom.exception;


import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.apache.tomcat.websocket.AuthenticationException;
import org.campus.classroom.common.Result;
import org.campus.classroom.enums.ResultCode;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 捕获 JWT 过期异常
     */
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
        e.printStackTrace();
        return Result.fail(ResultCode.INTERNAL_ERROR, "系统内部错误");
    }
}