package com.rishu.workflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rishu.workflow.dto.OutboxEventResponseDto;
import com.rishu.workflow.entity.OutboxEvent;
import com.rishu.workflow.enums.OutboxEventStatus;
import com.rishu.workflow.event.RequestLifecycleEvent;
import com.rishu.workflow.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class OutboxService {

    private final OutboxProcessorService outboxProcessorService;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public void save(RequestLifecycleEvent event) {

        try{

            String payload = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent =  OutboxEvent.builder()
                    .eventType("REQUEST_LIFECYCLE")
                    .aggregateId(event.requestId())
                    .payload(payload)
                    .status(OutboxEventStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .retryCount(0)
                    .build();

            outboxEventRepository.save(outboxEvent);
        }
        catch (JsonProcessingException e){
            throw new IllegalStateException(
                    "Failed to serialize request lifecycle event", e
            );
        }
    }

    public OutboxEventResponseDto manuallyRetry(Long id){
        return outboxProcessorService.retryPermanentlyFailedEvent(id);
    }
}
