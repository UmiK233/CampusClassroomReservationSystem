package org.campus.classroom.service;

import org.campus.classroom.dto.ChangePasswordDTO;
import org.campus.classroom.dto.EmailCodeSendDTO;
import org.campus.classroom.dto.EmailLoginDTO;
import org.campus.classroom.dto.RegisterDTO;
import org.campus.classroom.dto.ResetPasswordByCodeDTO;
import org.campus.classroom.dto.UpdateNicknameDTO;
import org.campus.classroom.security.LoginUser;
import org.campus.classroom.vo.LoginVO;
import org.campus.classroom.vo.UserInfoVO;

public interface AuthService {
    void sendEmailCode(EmailCodeSendDTO request);

    void register(RegisterDTO request);

    void validateLoginAccount(String username);

    LoginVO login(LoginUser loginUser);

    LoginVO loginByEmailCode(EmailLoginDTO request);

    LoginVO refresh(String refreshToken);

    void logout(String refreshToken);

    UserInfoVO getUserInfo(LoginUser token);

    void changePassword(Long userId, ChangePasswordDTO request);

    void resetPasswordByEmailCode(ResetPasswordByCodeDTO request);

    UserInfoVO updateNickname(Long userId, UpdateNicknameDTO request);
}
