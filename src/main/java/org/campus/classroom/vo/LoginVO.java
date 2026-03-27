package org.campus.classroom.vo;

import lombok.Data;

@Data
public class LoginVO {
    private String token;
    private UserInfoVO userInfo;
}
