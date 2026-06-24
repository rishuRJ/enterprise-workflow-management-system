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

//    @ManyToOne
    //@JoinColumn(name ="employee_id")
    private Long employeeId;

//    @ManyToOne
    private Long managerId;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}