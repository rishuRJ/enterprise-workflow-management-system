package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.mapper.RequestMapper;
import com.rishu.workflow.repository.RequestRepository;
import com.rishu.workflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final CurrentUserService currentUserService;
    private final RequestResponseDto requestResponseDto;
    private final RequestMapper requestMapper;

    @Override
    public RequestResponseDto createRequest(CreateRequestDto dto) {

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

        Request savedRequest = requestRepository.save(request);

        return requestMapper.toDto(savedRequest);
    }

    @Override
    public List<RequestResponseDto> getAllRequest() {
//        List<RequestResponseDto> requestResponseDtos = new ArrayList<>();
//        List<Request> requests = requestRepository.findAll();
//        for(Request request : requests) {
//            requestResponseDtos.add(requestMapper.toDto(request));
//        }
//        return requestResponseDtos;

        return requestRepository.findAll()
                .stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    public RequestResponseDto getRequestById(Long id) {

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

        return requestMapper.toDto(request);

    }

    @Override
    public RequestResponseDto approveRequest(Long id) {

        Request request =
                validateManagerAndRequest(id);

        request.setStatus(RequestStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        return requestMapper.toDto(savedRequest);
    }

    @Override
    public RequestResponseDto rejectRequest(Long id) {

        Request request =
                validateManagerAndRequest(id);

        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());

        Request savedRequest = requestRepository.save(request);
        return requestMapper.toDto(savedRequest);
    }

    @Override
    public List<RequestResponseDto> getMyRequests() {

        User currentUser =
                currentUserService.getCurrentUser();

        return requestRepository
                .findByEmployeeId(currentUser.getId())
                .stream()
                .map(requestMapper::toDto)
                .toList();
    }

    @Override
    public List<RequestResponseDto> getManagerRequests() {

        User currentUser =
                currentUserService.getCurrentUser();

        if (currentUser.getRole() != Role.ROLE_MANAGER) {

            throw new AccessDeniedException(
                    "Only managers can view assigned requests");
        }

        return requestRepository
                .findByManagerId(currentUser.getId())
                .stream()
                .map(requestMapper::toDto)
                .toList();
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