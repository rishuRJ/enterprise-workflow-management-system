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

    @Version
    private Long version;

    private String title;

    private String description;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name ="employee_id")
    private User employee;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name  = "manager_id")
    private User manager;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}