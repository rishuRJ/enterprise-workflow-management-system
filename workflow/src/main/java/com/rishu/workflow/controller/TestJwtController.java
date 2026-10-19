package com.rishu.workflow.controller;

import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.security.JwtService;
import com.rishu.workflow.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class TestJwtController {

    private final JwtService jwtService;

    @GetMapping("/token")
    public String token() {
        return jwtService.generateToken("rishu@gmail.com");
    }

    @GetMapping("/email")
    public String email() {

        String token =
                jwtService.generateToken("rishu@gmail.com");

        return jwtService.extractEmail(token);
    }
}
