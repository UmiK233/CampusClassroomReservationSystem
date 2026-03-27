package org.campus.classroom.service;

import jakarta.servlet.http.HttpServletRequest;
import org.campus.classroom.dto.LoginDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;


public interface AuthService {
    void register(RegisterDTO request);
    LoginVO login(LoginDTO request);
    UserInfoVO getUserInfo(String token);
}
