package com.rishu.workflow.dto;

import com.rishu.workflow.enums.OutboxEventStatus;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Data
@Builder
public class OutboxEventResponseDto {

    private Long eventId;

    private OutboxEventStatus status;

    private String eventType;

    private Long aggregateId;

    private int retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    private String lastError;
}
