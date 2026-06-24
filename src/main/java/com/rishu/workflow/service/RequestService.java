package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.exception.ResourceNotFoundException;

import java.util.List;

public interface RequestService {
    Request createRequest(CreateRequestDto requestDto);

    List<Request> getAllRequest();

    Request getRequestById(Long id);

    Request approveRequest(Long id);

    Request rejectRequest(Long id);

    List<Request> getMyRequests();

    List<Request> getManagerRequests();
}
