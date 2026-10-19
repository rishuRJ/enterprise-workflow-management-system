package com.rishu.workflow.controller;


import com.rishu.workflow.dto.OutboxEventResponseDto;
import com.rishu.workflow.service.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final OutboxService outboxService;

    @PostMapping("outbox/{id}/retry")
    public OutboxEventResponseDto retry(@PathVariable Long id){
        return outboxService.manuallyRetry(id);
    }
}
