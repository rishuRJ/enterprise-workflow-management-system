package com.rishu.workflow.dto;

import lombok.Data;

@Data
public class CreateRequestDto {

    private String title;

    private String description;

    private Long employeeId;

    private Long managerId;
}