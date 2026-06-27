package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.exception.ResourceNotFoundException;

import java.util.List;

public interface RequestService {
    RequestResponseDto createRequest(CreateRequestDto requestDto);

    List<RequestResponseDto> getAllRequest();

    RequestResponseDto getRequestById(Long id);

    RequestResponseDto approveRequest(Long id);

    RequestResponseDto rejectRequest(Long id);

    List<RequestResponseDto> getMyRequests();

    List<RequestResponseDto> getManagerRequests();
}
