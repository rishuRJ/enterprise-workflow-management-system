package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.repository.RequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;

    @Override
    public Request createRequest(CreateRequestDto dto) {
        Request request = Request.builder()
                .title(dto.getTitle())
                .description(dto.getDescription())
                .employeeId(dto.getEmployeeId())
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

        return requestRepository.findById(id).orElseThrow(() ->
                new RuntimeException(
                        "Request not found"));
    }

    @Override
    public Request approveRequest(Long id) {

        Request request = requestRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Request not found"));

        if(request.getStatus()!=RequestStatus.PENDING){
            throw new RuntimeException("Request is already approved");
        }
        request.setStatus(RequestStatus.APPROVED);
        request.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }

    @Override
    public Request rejectRequest(Long id) {
        Request request = requestRepository.findById(id).orElseThrow(() ->
                new RuntimeException("Request not found"));

        if(request.getStatus()!=RequestStatus.PENDING){
            throw new RuntimeException("Request is already approved");
        }
        request.setStatus(RequestStatus.REJECTED);
        request.setUpdatedAt(LocalDateTime.now());

        return requestRepository.save(request);
    }
}
