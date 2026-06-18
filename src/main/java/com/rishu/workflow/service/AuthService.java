package com.rishu.workflow.service;

import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.entity.User;

public interface AuthService {

    User register(RegisterRequest request);
}