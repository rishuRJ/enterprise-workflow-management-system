package com.rishu.workflow.mapper;

import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.entity.Request;
import lombok.Builder;
import org.springframework.stereotype.Component;

@Component
public class RequestMapper {

    public RequestResponseDto toDto(Request request){
        return RequestResponseDto.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .employeeId(request.getEmployeeId())
                .managerId(request.getManagerId()).status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

}
