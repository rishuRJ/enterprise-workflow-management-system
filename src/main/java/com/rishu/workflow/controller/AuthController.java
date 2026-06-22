package com.rishu.workflow.controller;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.LoginRequest;
import com.rishu.workflow.dto.LoginResponse;
import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public User register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }


}