package org.campus.classroom.campusclassroomreservationsystem;

import org.campus.classroom.mapper.UserMapper;
import org.campus.classroom.utils.JWTUtils;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class CampusClassroomReservationSystemApplicationTests {
    @Autowired
    private UserMapper userMapper;
    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration}")
    private long expirationTime;

    @Autowired
    private JWTUtils jwtUtils;

    @Test
    void contextLoads() {
        String token=jwtUtils.generateToken(123L, "testuser", "USER", expirationTime);
        System.out.println("Generated JWT: " + token);
        System.out.println(jwtUtils.parseToken(token));
    }

}
