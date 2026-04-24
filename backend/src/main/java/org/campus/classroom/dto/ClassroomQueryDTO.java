package org.campus.classroom.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ClassroomQueryDTO {
    @Size(max = 50, message = "教室编号长度不能超过50")
    private String roomNumber;

    @Size(max = 100, message = "教学楼长度不能超过100")
    private String building;

    @Pattern(regexp = "ENABLED|DISABLED", message = "状态只能是 ENABLED 或 DISABLED")
    private String status;
}
