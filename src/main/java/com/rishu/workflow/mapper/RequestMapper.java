package com.rishu.workflow.mapper;

import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.dto.UserSummaryDto;
import com.rishu.workflow.entity.Request;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RequestMapper {

    private final UserMapper userMapper;

    public RequestResponseDto toDto(Request request){
        return RequestResponseDto.builder()
                .id(request.getId())
                .title(request.getTitle())
                .description(request.getDescription())
                .employee(userMapper.toSummaryDto(request.getEmployee()))
                .manager(userMapper.toSummaryDto(request.getManager()))
                .status(request.getStatus())
                .createdAt(request.getCreatedAt())
                .updatedAt(request.getUpdatedAt())
                .build();
    }

}
