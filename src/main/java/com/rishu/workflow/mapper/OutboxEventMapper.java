package com.rishu.workflow.mapper;


import com.rishu.workflow.dto.OutboxEventResponseDto;
import com.rishu.workflow.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OutboxEventMapper {

    public OutboxEventResponseDto toDto(OutboxEvent outboxEvent){

        return OutboxEventResponseDto.builder()
                .eventId(outboxEvent.getId())
                .aggregateId(outboxEvent.getAggregateId())
                .retryCount(outboxEvent.getRetryCount())
                .eventType(outboxEvent.getEventType())
                .nextRetryAt(outboxEvent.getNextRetryAt())
                .processedAt(outboxEvent.getProcessedAt())
                .lastError(outboxEvent.getLastError())
                .createdAt(outboxEvent.getCreatedAt())
                .status(outboxEvent.getStatus())
                .build();
    }
}
