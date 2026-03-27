package org.campus.classroom.common;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result<T> {

    private Integer code;
    private String message;
    private T data;

    //无返回数据成功
    public static <T> Result<T> success(String message) {
        return new Result<>(200, message, null);
    }
    //成功并返回数据
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(200,message,data);
    }

    //内部错误
    public static <T> Result<T> fail(String message) {
        return new Result<>(500, message, null);
    }
    //业务错误
    public static <T> Result<T> fail(Integer code, String message) {
        return new Result<>(code, message, null);
    }
}