package org.campus.classroom.campusclassroomreservationsystem;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;


@SpringBootApplication
@ComponentScan("org.campus.classroom.controller")
@MapperScan("org.campus.classroom.mapper")
public class CampusClassroomReservationSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(CampusClassroomReservationSystemApplication.class, args);
    }

}
