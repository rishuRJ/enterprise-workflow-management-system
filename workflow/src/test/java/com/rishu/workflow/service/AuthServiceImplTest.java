package com.rishu.workflow.service;
import com.rishu.workflow.dto.LoginRequest;
import com.rishu.workflow.dto.LoginResponse;
import com.rishu.workflow.dto.RegisterRequest;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.exception.DuplicateResourceException;
import com.rishu.workflow.mapper.UserMapper;
import com.rishu.workflow.repository.UserRepository;
import com.rishu.workflow.security.JwtService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private UserMapper userMapper;
    @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void register_throwsWhenEmailAlreadyExists() {
        // arrange
        RegisterRequest request = RegisterRequest.builder().email("raj@gmail.com").build();

        when(userRepository.findByEmail("raj@gmail.com"))
                .thenReturn(Optional.of(new User()));

        // act + assert
        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_savesUserWithEncodedPassword() {
        RegisterRequest request = RegisterRequest.builder()
                .email("raj@gmail.com")
                .name("raj")
                .role(Role.ROLE_EMPLOYEE)
                .password("real-password")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());

        User savedUser = captor.getValue();

        assertThat(savedUser.getPassword()).isEqualTo("encoded-password");
        assertThat(savedUser.getEmail()).isEqualTo(request.getEmail());
        assertThat(savedUser.getRole()).isEqualTo(request.getRole());
    }

    @Test
    void login_returnsTokenFromJwtService() {

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("raj@gmail.com");

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(jwtService.generateToken(authentication.getName())).thenReturn("token");



        LoginResponse loginResponse = authService.login(new LoginRequest());

        assertThat(loginResponse.getToken()).isEqualTo("token");
    }

    @Test
    void login_throwsWhenCredentialsAreInvalid() {

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(new LoginRequest()))
                .isInstanceOf(BadCredentialsException.class);

        verifyNoInteractions(jwtService);
    }
}