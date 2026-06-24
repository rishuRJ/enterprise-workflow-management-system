package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final CurrentUserService currentUserService;

    @Override
    public Request createRequest(CreateRequestDto dto) {

        User user = currentUserService.getCurrentUser();

        Request request = Request.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .employeeId(user.getId())
                .managerId(dto.getManagerId())
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return requestRepository.save(request);
    }

    @Override
    public List<Request> getAllRequest() {
        return requestRepository.findAll();
    }

    @Override
    public Request getRequestById(Long id) {

        User user = currentUserService.getCurrentUser();

        Request request = requestRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Request not found"));

        if (!request.getEmployeeId().equals(user.getId())
                && !request.getManagerId().equals(user.getId())) {

            throw new AccessDeniedException(
                    "Access denied");
        }

        return request;
    }

    @Override
    public Request approveRequest(Long id) {

        Request request =
                validateManagerAndRequest(id);

        request.setStatus(RequestStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Override
    public Request rejectRequest(Long id) {

        Request request =
                validateManagerAndRequest(id);

        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Override
    public List<Request> getMyRequests() {

        User currentUser =
                currentUserService.getCurrentUser();

        return requestRepository
                .findByEmployeeId(currentUser.getId());
    }

    @Override
    public List<Request> getManagerRequests() {

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ROLE_MANAGER) {

            throw new AccessDeniedException(
                    "Only managers can view assigned requests");
        }

        return requestRepository
                .findByManagerId(currentUser.getId());
    }

    private Request validateManagerAndRequest(Long id) {

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ROLE_MANAGER) {

            throw new AccessDeniedException(
                    "Only managers can approve requests");
        }

        Request request =
                requestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        if (!request.getManagerId()
                .equals(currentUser.getId())) {

            throw new AccessDeniedException(
                    "You are not assigned to this request");
        }

        if (request.getStatus() != RequestStatus.PENDING) {

            throw new BusinessException(
                    "Request already processed");
        }

        return request;
    }
}