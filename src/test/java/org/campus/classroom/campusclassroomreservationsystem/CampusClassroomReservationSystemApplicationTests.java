package org.campus.classroom.campusclassroomreservationsystem;

import org.campus.classroom.entity.Classroom;
import org.campus.classroom.entity.Reservation;
import org.campus.classroom.entity.Seat;
import org.campus.classroom.enums.ResourceType;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.mapper.ReservationMapper;
import org.campus.classroom.mapper.SeatMapper;
import org.campus.classroom.service.ClassroomService;
import org.campus.classroom.service.ReservationService;
import org.campus.classroom.vo.ClassroomVO;
import org.campus.classroom.vo.SeatVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@SpringBootTest
class CampusClassroomReservationSystemApplicationTests {
    @Autowired
    ClassroomService service;
    @Autowired
    ClassroomMapper classroomMapper;
    @Autowired
    SeatMapper seatMapper;

    @Autowired
    ReservationService reservationService;
    @Autowired
    private ReservationMapper reservationMapper;

    private ClassroomVO classroomToClassroomVO(Classroom classroom) {
        ClassroomVO classroomVO = new ClassroomVO();
        BeanUtils.copyProperties(classroom, classroomVO);
        classroomVO.setCapacity(classroom.getSeatRows() * classroom.getSeatCols());
        return classroomVO;
    }

    @Test
    void contextLoads() {
        List<Reservation> reservationList = reservationMapper.selectByUserId(10004L);
        // 1. 收集所有 classroomId
        Set<Long> classroomIds = reservationList.stream()
                .map(Reservation::getClassroomId)
                .collect(Collectors.toSet());

        // 2. 收集所有 seatId（只有座位预约时收集）
        Set<Long> seatIds = reservationList.stream()
                .filter(reservation -> ResourceType.SEAT.name().equals(reservation.getResourceType()))
                .map(Reservation::getResourceId)
                .collect(Collectors.toSet());

        // 3. 批量查 classroom
        Map<Long, ClassroomVO> classroomVOMap = classroomIds.isEmpty()
                ? Collections.emptyMap()
                : classroomMapper.selectByIds(classroomIds).stream().map(this::classroomToClassroomVO)
                .collect(Collectors.toMap(ClassroomVO::getId, Function.identity()));
        System.out.println(classroomVOMap);

//        // 4. 批量查 seat
//        Map<Long, SeatVO> seatVOMap = seatIds.isEmpty()
//                ? Collections.emptyMap()
//                : seatMapper.selectByIds(seatIds).stream().map(this::seatToSeatVO)
//                .collect(Collectors.toMap(SeatVO::getId, Function.identity()));

    }

}
