package org.campus.classroom.service;

import org.campus.classroom.dto.RegisterRequest;
import org.springframework.stereotype.Service;


public interface AuthService {
    void register(RegisterRequest request);
}
