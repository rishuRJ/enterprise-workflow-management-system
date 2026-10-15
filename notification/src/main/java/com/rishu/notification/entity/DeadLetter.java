package com.rishu.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "dead_letters",
        uniqueConstraints = @UniqueConstraint(columnNames = {"dlt_partition", "dlt_offset"}))
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeadLetter {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String messageKey;

    @Column(columnDefinition = "text")
    private String payload;

    private String exceptionClass;

    @Column(columnDefinition = "text")
    private String exceptionMessage;

    private String originalTopic;

    @Column(name = "dlt_partition")
    private int dltPartition;

    @Column(name = "dlt_offset")
    private long dltOffset;

    private LocalDateTime receivedAt;
}
