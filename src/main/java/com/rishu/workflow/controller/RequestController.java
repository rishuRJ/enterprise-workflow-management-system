package com.rishu.workflow.controller;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.service.RequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/requests")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    public Request RequestResponseDto(@Valid @RequestBody CreateRequestDto dto) {
        return requestService.createRequest(dto);
    }

    @GetMapping("/manager")
    public List<RequestResponseDto> getManagerRequests() {
        return requestService.getManagerRequests();
    }

    @GetMapping("/my")
    public List<RequestResponseDto> getMyRequests(){
        return requestService.getMyRequests();
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
