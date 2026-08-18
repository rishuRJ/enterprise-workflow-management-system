package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.dto.RequestSearchDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;
import com.rishu.workflow.exception.BusinessException;
import com.rishu.workflow.exception.ResourceNotFoundException;
import com.rishu.workflow.mapper.RequestMapper;
import com.rishu.workflow.repository.RequestRepository;
import com.rishu.workflow.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

import static com.rishu.workflow.specification.RequestSpecifications.*;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final CurrentUserService currentUserService;
    private final RequestMapper requestMapper;
    private final UserRepository userRepository;
    private final RequestHistoryService requestHistoryService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public RequestResponseDto createRequest(CreateRequestDto dto) {

        User user = currentUserService.getCurrentUser();

        if (user.getRole() == Role.ROLE_ADMIN) {
            throw new AccessDeniedException("Administrators cannot create requests");
        }

        User manager = userRepository.findById(dto.getManagerId())
                .orElseThrow(() -> new ResourceNotFoundException("No manager with this ID"));;

        if (manager.getRole() != Role.ROLE_MANAGER) {
            throw new BusinessException("The selected user is not a manager");
        }

        Request request = Request.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .employee(user)
                .manager(manager)
                .status(RequestStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Request savedRequest = requestRepository.save(request);

        return requestMapper.toDto(savedRequest);
    }

    @Override
    @PreAuthorize("hasRole('ADMIN')")
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



        if (!request.getEmployee().getId().equals(user.getId())
                && !request.getManager().getId().equals(user.getId())) {

            throw new AccessDeniedException(
                    "Access denied");
        }

        return requestMapper.toDto(request);

    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public RequestResponseDto approveRequest(Long id) {

        User currentUser = currentUserService.getCurrentUser();
        Request request =
                validateManagerAndRequest(id, currentUser);

        RequestStatus previousStatus = request.getStatus();

        request.setStatus(RequestStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());

        requestHistoryService.saveRequestHistory(request,currentUser,"REQUEST_APPROVED", previousStatus);
//        Request savedRequest = requestRepository.save(request);
        return requestMapper.toDto(request);
    }
    @Override
    @Transactional
    @PreAuthorize("hasRole('MANAGER')")
    public RequestResponseDto rejectRequest(Long id) {

        User currentUser = currentUserService.getCurrentUser();
        Request request =
                validateManagerAndRequest(id, currentUser);

        RequestStatus previousStatus = request.getStatus();

        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());

        requestHistoryService.saveRequestHistory(request,currentUser,"REQUEST_REJECTED", previousStatus);

//        Request savedRequest = requestRepository.save(request);
        return requestMapper.toDto(request);
    }

    @Override
    public Page<RequestResponseDto> getMyRequests(RequestSearchDto searchDto, Pageable pageable)  {

        User currentUser =
                currentUserService.getCurrentUser();
        Specification<Request> spec = Specification.allOf(
                hasEmployee(currentUser),
                hasStatuses(searchDto.getStatuses()),
                hasManagerId(searchDto.getManagerId()),
                titleContains(searchDto.getTitle())
        );

        Page<Request> requests = requestRepository.findAll(spec, pageable);
                //requestRepository.findByEmployee(spec, pageable);

        return requests.map(requestMapper::toDto);
    }

    @Override
    @PreAuthorize("hasRole('MANAGER')")
    public Page<RequestResponseDto> getManagerRequests(
            RequestSearchDto searchDto,
            Pageable pageable) {

        User currentUser =
                currentUserService.getCurrentUser();

        Specification<Request> spec = Specification.allOf(
                hasManager(currentUser),
                hasStatuses(searchDto.getStatuses()),
                titleContains(searchDto.getTitle())
        );

        Page<Request> requests = requestRepository.findAll(spec, pageable);

        return  requests.map(requestMapper::toDto);
    }

    private Request validateManagerAndRequest(Long id, User currentUser) {

        if (currentUser.getRole() != Role.ROLE_MANAGER) {

            throw new AccessDeniedException(
                    "Only managers can approve requests");
        }

        Request request =
                requestRepository.findById(id)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Request not found"));

        if (!request.getManager().getId()
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
