package org.campus.classroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AdminUserStatusUpdateDTO {
    @NotNull(message = "status不能为空")
    @Min(value = 0, message = "status只能为0或1")
    @Max(value = 1, message = "status只能为0或1")
    private Integer status;

    @Size(max = 255, message = "通知内容不能超过255个字符")
    private String reason;
}
