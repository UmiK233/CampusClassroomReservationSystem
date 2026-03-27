package org.campus.classroom.campusclassroomreservationsystem;

import org.campus.classroom.dto.ClassroomUpdateDTO;
import org.campus.classroom.mapper.ClassroomMapper;
import org.campus.classroom.service.ClassroomService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusClassroomReservationSystemApplicationTests {
    @Autowired
    ClassroomService service;
    @Autowired
    ClassroomMapper mapper;

    @Test
    void contextLoads() {
        System.out.println(mapper.selectByBuildingAndNumber("数智楼", "222"));
    }

}
