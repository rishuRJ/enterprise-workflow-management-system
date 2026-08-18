package com.rishu.workflow.entity;


import com.rishu.workflow.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_history")
@RequiredArgsConstructor
@Builder
@Getter
@Setter
@AllArgsConstructor

public class RequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "request_id")
    private Request request;

    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    private String action;

    @Enumerated(EnumType.STRING)
    private RequestStatus previousStatus;

    @Enumerated(EnumType.STRING)
    private RequestStatus newStatus;

    private LocalDateTime occurredAt;


}
