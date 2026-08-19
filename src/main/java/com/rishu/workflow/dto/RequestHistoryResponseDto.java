package com.rishu.workflow.dto;


import com.rishu.workflow.enums.RequestStatus;
import lombok.Builder;
import lombok.Data;


import java.time.LocalDateTime;

@Builder
@Data
public class RequestHistoryResponseDto {

    private Long requestId;

    private UserSummaryDto actor;

    private String action;

    private RequestStatus previousStatus;

    private RequestStatus newStatus;

    private LocalDateTime occurredAt;
}
