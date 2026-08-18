package com.rishu.workflow.service;

import com.rishu.workflow.dto.LoginRequest;
import com.rishu.workflow.dto.LoginResponse;
import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.dto.UserResponseDto;
import com.rishu.workflow.entity.User;

public interface AuthService {

    UserResponseDto register(RegisterRequest request);

    LoginResponse login(LoginRequest request);
}