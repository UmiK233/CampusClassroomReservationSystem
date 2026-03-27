package org.campus.classroom.exception;


import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.campus.classroom.common.Result;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public Result<Void> handleBusinessException(BusinessException e) {
        return Result.fail(e.getCode(), e.getMessage());
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

        return Result.fail(400, message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolationException(ConstraintViolationException e) {
        // 1. 从异常中获取校验失败信息
        String message = e.getConstraintViolations() // 拿到所有路径/查询参数的校验失败信息
                .stream()
                .findFirst() // 只取第一个错误
                .map(ConstraintViolation::getMessage) // 拿到错误提示（比如 "id 不能小于1"）
                .orElse("参数校验失败"); // 默认提示

        return Result.fail(400, message);
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        e.printStackTrace();
        return Result.fail(500, "系统内部错误");
    }
}