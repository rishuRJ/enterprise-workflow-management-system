package com.rishu.workflow.controller;

import com.rishu.workflow.dto.CreateRequestDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.service.RequestService;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RequiredArgsConstructor
@RestController
@RequestMapping("/request")
public class RequestController {
    private final RequestService requestService;

    @PostMapping
    public Request request(@Valid @RequestBody CreateRequestDto dto) {
        return requestService.createRequest(dto);
    }

    @GetMapping
    public List<Request> getAllRequest() {
        return requestService.getAllRequest();
    }

    @GetMapping("/{id}")
    public Request getRequest(@PathVariable Long id) {
        return requestService.getRequestById(id);
    }

    @PutMapping("/{id}/approve")
    public Request approveRequest(@PathVariable Long id) {
        return requestService.approveRequest(id);
    }

    @PutMapping("/{id}/reject")
    public Request rejectRequest(@PathVariable Long id) {
        return requestService.rejectRequest(id);
    }
}
