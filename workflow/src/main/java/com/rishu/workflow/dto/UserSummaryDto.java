package com.rishu.workflow.dto;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSummaryDto {
    private Long id;
    private String name;
    private String email;
}

