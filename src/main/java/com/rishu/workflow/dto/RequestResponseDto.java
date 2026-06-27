package com.rishu.workflow.dto;

import com.rishu.workflow.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestResponseDto {

    private Long id;

    private String title;

    private String description;

    private Long employeeId;

    private Long managerId;

    private RequestStatus status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
