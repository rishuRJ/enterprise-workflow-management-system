package com.rishu.workflow.repository;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import com.rishu.workflow.AbstractIntegrationTest;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Role;

class UserRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RequestHistoryRepository requestHistoryRepository;

    @Autowired
    private RequestRepository requestRepository;

    @BeforeEach
    void setUp() {
        requestHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rejectsASecondUserWithTheSameEmail() {
        userRepository.save(newUser("alice@test.com"));

        assertThatThrownBy(() -> {userRepository.save(newUser("alice@test.com"));})
                .isInstanceOf(DataIntegrityViolationException.class);
    }


    private User newUser(String email) {
        return User.builder()
                .name("Alice")
                .email(email)
                .password("not-used")
                .role(Role.ROLE_EMPLOYEE)
                .createdAt(LocalDateTime.now())
                .build();
    }
}