package com.rishu.workflow.service;

import com.rishu.workflow.dto.LoginRequest;
import com.rishu.workflow.dto.LoginResponse;
import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.repository.UserRepository;
import com.rishu.workflow.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    public User register(RegisterRequest request) {

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(request.getRole())
                .createdAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        boolean validPassword =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword());

        if (!validPassword) {
            throw new RuntimeException("Invalid credentials");
        }

        String token =
                jwtService.generateToken(user.getEmail());

        return new LoginResponse(token);
    }
}