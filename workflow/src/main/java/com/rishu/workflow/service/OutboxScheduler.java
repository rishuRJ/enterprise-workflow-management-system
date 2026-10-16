package com.rishu.workflow.service;


import com.rishu.workflow.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxScheduler {
    private final OutboxProcessorService outboxProcessorService;

    @Scheduled(fixedRate = 5000)
    public void processOutbox() {

        outboxProcessorService.recoverStaleEvents();

        List<OutboxEvent> events = outboxProcessorService.claimEvents(10);
        for (OutboxEvent event : events) {
            outboxProcessorService.process(event);
        }
    }
}
