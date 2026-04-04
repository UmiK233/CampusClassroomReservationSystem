package org.campus.classroom.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String nickname;
    private String email;
    private String role;
    private Integer status;
    //插入自动生成,取出时需要对应
    private LocalDateTime createTime;
}
