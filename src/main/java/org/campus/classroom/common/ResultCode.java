package org.campus.classroom.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ResultCode {
    SUCCESS(200, "success"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或Token失效"),
    FORBIDDEN(403, "无权限访问"),
    CONFLICT(409, "资源冲突"),
    ERROR(500, "系统内部错误");

    private final int code;
    private final String message;
}
