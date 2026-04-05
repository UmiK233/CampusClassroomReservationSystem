package org.campus.classroom.vo;

import lombok.Data;
import org.campus.classroom.enums.ReservationStatus;
import org.campus.classroom.enums.ResourceType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class ReservationVO {
    private Long id;
    private String resourceType;
    private Long resourceId;
    private String resourceName;
    private LocalDate reserveDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String status;
    private LocalDateTime createTime;
}
