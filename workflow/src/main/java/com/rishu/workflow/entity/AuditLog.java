package com.rishu.workflow.entity;

import jakarta.persistence.*;
import com.rishu.workflow.enums.RequestStatus;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Getter
@Setter
@Table(name = "AuditLog")
@Entity
public class AuditLog {


    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private int id;

    private String action;

    private Long requestId;

    private String actorEmail;

    private String employeeEmail;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    private LocalDateTime createdAt;


}
