package com.rishu.workflow.entity;


import com.rishu.workflow.enums.OutboxEventStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Table(name = "outbox_event")
@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private OutboxEventStatus status;

    private String eventType;

    private Long aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = ("jsonb"))
    private String payload;

    private int retryCount;

    private LocalDateTime nextRetryAt;

    private LocalDateTime processingStartedAt;

    private LocalDateTime createdAt;

    private LocalDateTime processedAt;

    @Column(length = 2000)
    private String lastError;

}
