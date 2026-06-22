package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.Request;

import java.util.List;

public interface RequestService {
    Request createRequest(
            CreateRequestDto requestDto);

    List<Request> getAllRequest();

    Request getRequestById(Long id);

    Request approveRequest(Long id);

    Request rejectRequest(Long id);
}
