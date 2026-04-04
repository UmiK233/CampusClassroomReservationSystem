package org.campus.classroom.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class ClassroomReservationCreateDTO {
    @NotNull(message = "教室ID不能为空")
    private Long classroomId;
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;
    @Size(max = 255, message = "备注长度不能超过255")
    private String reason;
}