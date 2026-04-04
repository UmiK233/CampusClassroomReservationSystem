package org.campus.classroom.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class Reservation {
    private Long id;
    private Long userId;

    // 当 resourceType=SEAT 时，对应 seat.id
    // 当 resourceType=CLASSROOM 时，对应 classroom.id
    private String resourceType;
    private Long resourceId;

    //冗余字段设计
    private Long classroomId;

    //预约时间相关字段
    private LocalDate reserveDate;
    private LocalTime startTime;
    private LocalTime endTime;

    private String reason;

    private String status;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}