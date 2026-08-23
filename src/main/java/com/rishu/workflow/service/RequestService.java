package com.rishu.workflow.service;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestHistoryResponseDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.dto.RequestSearchDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RequestService {
    RequestResponseDto createRequest(CreateRequestDto requestDto);

    List<RequestResponseDto> getAllRequest();

    RequestResponseDto getRequestById(Long id);

    List<RequestHistoryResponseDto> getRequestHistory(Long id);

    RequestResponseDto approveRequest(Long id);

    RequestResponseDto rejectRequest(Long id);

    Page<RequestResponseDto> getMyRequests(RequestSearchDto searchDto, Pageable pageable);

    Page<RequestResponseDto> getManagerRequests(
            RequestSearchDto searchDto,
            Pageable pageable);
}
