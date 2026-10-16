package com.rishu.workflow.repository;

import static com.rishu.workflow.specification.RequestSpecifications.hasEmployee;
import static com.rishu.workflow.specification.RequestSpecifications.hasManager;
import static com.rishu.workflow.specification.RequestSpecifications.hasStatuses;
import static com.rishu.workflow.specification.RequestSpecifications.titleContains;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import com.rishu.workflow.AbstractIntegrationTest;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;

/**
 * Runs the request queries against real PostgreSQL: the Specifications behind
 * GET /requests/my and /requests/manager, pagination and sorting, and the
 * optimistic lock on concurrent updates.
 */
class RequestRepositoryIT extends AbstractIntegrationTest {

    private static final PageRequest NEWEST_FIRST =
            PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

    @Autowired
    private RequestRepository requestRepository;

    @Autowired
    private RequestHistoryRepository requestHistoryRepository;

    @Autowired
    private UserRepository userRepository;

    private User alice;
    private User bob;
    private User manager;

    @BeforeEach
    void setUp() {
        requestHistoryRepository.deleteAll();
        requestRepository.deleteAll();
        userRepository.deleteAll();

        alice = saveUser("alice@test.com", Role.ROLE_EMPLOYEE);
        bob = saveUser("bob@test.com", Role.ROLE_EMPLOYEE);
        manager = saveUser("manager@test.com", Role.ROLE_MANAGER);
    }

    @Test
    void myRequestsReturnsOnlyTheEmployeesRequestsNewestFirst() {
        saveRequest(alice, "Old laptop", RequestStatus.PENDING, 3);
        saveRequest(alice, "Middle monitor", RequestStatus.APPROVED, 2);
        saveRequest(alice, "New keyboard", RequestStatus.PENDING, 1);
        saveRequest(bob, "Bob's chair", RequestStatus.PENDING, 0);

        Page<Request> page = requestRepository.findAll(
                hasEmployee(alice), PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "createdAt")));

        assertThat(page.getContent())
                .extracting(Request::getTitle)
                .containsExactly("New keyboard", "Middle monitor");
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }

    @Test
    void filtersByStatusAndTitleIgnoringCase() {
        saveRequest(alice, "Laptop charger", RequestStatus.PENDING, 1);
        saveRequest(alice, "Laptop bag", RequestStatus.REJECTED, 2);
        saveRequest(alice, "Desk lamp", RequestStatus.PENDING, 3);

        Specification<Request> spec = Specification.allOf(
                hasEmployee(alice),
                hasStatuses(List.of(RequestStatus.PENDING)),
                titleContains("LAPTOP"));

        assertThat(requestRepository.findAll(spec, NEWEST_FIRST).getContent())
                .extracting(Request::getTitle)
                .containsExactly("Laptop charger");
    }

    @Test
    void managerSeesRequestsFromAllTheirEmployees() {
        saveRequest(alice, "Alice request", RequestStatus.PENDING, 2);
        saveRequest(bob, "Bob request", RequestStatus.PENDING, 1);

        assertThat(requestRepository.findAll(hasManager(manager), NEWEST_FIRST).getContent())
                .extracting(Request::getTitle)
                .containsExactly("Bob request", "Alice request");
    }

    @Test
    void secondUpdateOfAStaleCopyFailsWithOptimisticLock() {
        Long id = saveRequest(alice, "Laptop", RequestStatus.PENDING, 0).getId();

        // Two managers open the same request at the same time
        Request firstCopy = requestRepository.findById(id).orElseThrow();
        Request secondCopy = requestRepository.findById(id).orElseThrow();

        firstCopy.setStatus(RequestStatus.APPROVED);
        requestRepository.save(firstCopy);

        secondCopy.setStatus(RequestStatus.REJECTED);
        assertThatThrownBy(() -> requestRepository.save(secondCopy))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);

        Request stored = requestRepository.findById(id).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(RequestStatus.APPROVED);
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    private User saveUser(String email, Role role) {
        return userRepository.save(User.builder()
                .name(email)
                .email(email)
                .password("not-used")
                .role(role)
                .createdAt(LocalDateTime.now())
                .build());
    }

    private Request saveRequest(User employee, String title, RequestStatus status, int daysAgo) {
        LocalDateTime createdAt = LocalDateTime.now().minusDays(daysAgo);
        return requestRepository.save(Request.builder()
                .title(title)
                .status(status)
                .employee(employee)
                .manager(manager)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build());
    }
}
