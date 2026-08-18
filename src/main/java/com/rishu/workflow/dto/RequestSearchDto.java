package com.rishu.workflow.dto;

import com.rishu.workflow.enums.RequestStatus;
import lombok.Data;

import java.util.List;

@Data
public class RequestSearchDto {

    private List<RequestStatus> statuses;

    private Long managerId;

    private String title;

}