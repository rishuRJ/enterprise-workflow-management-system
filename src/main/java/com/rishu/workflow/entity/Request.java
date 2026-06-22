package com.rishu.workflow.entity;

import com.rishu.workflow.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private Long employeeId;

    private Long managerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}