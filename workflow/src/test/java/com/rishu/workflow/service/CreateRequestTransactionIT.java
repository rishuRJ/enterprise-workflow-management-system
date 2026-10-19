package com.rishu.workflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import java.time.LocalDateTime;

import com.rishu.workflow.enums.OutboxEventStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.rishu.workflow.AbstractIntegrationTest;
import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.repository.OutboxEventRepository;
import com.rishu.workflow.repository.RequestHistoryRepository;
import com.rishu.workflow.repository.RequestRepository;
import com.rishu.workflow.repository.UserRepository;

class CreateRequestTransactionIT extends AbstractIntegrationTest {

    @Autowired private RequestService requestService;
    @Autowired private UserRepository userRepository;
    @Autowired private RequestRepository requestRepository;
    @Autowired private RequestHistoryRepository requestHistoryRepository;
    @Autowired private OutboxEventRepository outboxEventRepository;

    // The real OutboxService, but a test can make one of its methods fail
    @MockitoSpyBean private OutboxService outboxService;

    private Long managerId;

    @BeforeEach
    void setUp() {
        requestHistoryRepository.deleteAll();
        outboxEventRepository.deleteAll();
        requestRepository.deleteAll();

        userRepository.deleteAll();

        userRepository.save(user("alice@test.com", Role.ROLE_EMPLOYEE));
        managerId = userRepository.save(user("manager@test.com", Role.ROLE_MANAGER)).getId();
    }

    @Test
    @WithMockUser(username = "alice@test.com")   // act as if Alice is logged in
    void savesRequestHistoryAndOutboxEventTogether() {
        requestService.createRequest(new CreateRequestDto("New laptop", "Mine is broken", managerId));

        assertThat(requestRepository.count()).isEqualTo(1);
        assertThat(requestHistoryRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.count()).isEqualTo(1);
        assertThat(outboxEventRepository.findAll().getFirst().getStatus()).isEqualTo(OutboxEventStatus.PENDING);
    }

    @Test
    @WithMockUser(username = "alice@test.com")
    void rollsBackEverythingWhenTheOutboxInsertFails() {
        doThrow(new IllegalStateException("simulated outbox failure"))
                .when(outboxService).save(any());

        assertThatThrownBy(()->requestService.createRequest(new CreateRequestDto("New laptop", "Mine is broken", managerId)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(requestRepository.count()).isEqualTo(0);
        assertThat(requestHistoryRepository.count()).isEqualTo(0);
        assertThat(outboxEventRepository.count()).isEqualTo(0);
    }

    private User user(String email, Role role) {
        return User.builder().name(email).email(email).password("not-used")
                .role(role).createdAt(LocalDateTime.now()).build();
    }
}