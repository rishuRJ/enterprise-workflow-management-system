package com.rishu.workflow.controller;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.dto.RequestSearchDto;
import com.rishu.workflow.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/requests")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    public RequestResponseDto createRequest(@Valid @RequestBody CreateRequestDto dto) {
        return requestService.createRequest(dto);
    }

    @GetMapping("/manager")
    public Page<RequestResponseDto> getManagerRequests(RequestSearchDto searchDto,
                                                       @PageableDefault(
                                                               page = 0,
                                                               size = 20,
                                                               sort = "createdAt",
                                                               direction = Sort.Direction.DESC
                                                       )
                                                       Pageable pageable) {
        return requestService.getManagerRequests(searchDto, pageable);
    }

    @GetMapping("/my")
    Page<RequestResponseDto> getMyRequests(RequestSearchDto searchDto,
                                           @PageableDefault(
                                                   page = 0,
                                                   size = 20,
                                                   sort = "createdAt",
                                                   direction = Sort.Direction.DESC
                                           )
                                           Pageable pageable) {
        return requestService.getMyRequests(searchDto, pageable);
    }

    @GetMapping
    public List<RequestResponseDto> getAllRequest() {
        return requestService.getAllRequest();
    }

    @GetMapping("/{id}")
    public RequestResponseDto getRequest(@PathVariable Long id) {
        return requestService.getRequestById(id);
    }

    @PutMapping("/{id}/approve")
    public RequestResponseDto approveRequest(@PathVariable Long id) {
        return requestService.approveRequest(id);
    }

    @PutMapping("/{id}/reject")
    public RequestResponseDto rejectRequest(@PathVariable Long id) {
        return requestService.rejectRequest(id);
    }
}
