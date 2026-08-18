package com.rishu.workflow.dto;

import com.rishu.workflow.enums.Role;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
}

