package com.rishu.workflow.mapper;

import com.rishu.workflow.dto.UserResponseDto;
import com.rishu.workflow.dto.UserSummaryDto;
import com.rishu.workflow.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
public class UserMapper {
    public UserResponseDto toDto(User user) {
        return UserResponseDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .build();

    }

    public UserSummaryDto toSummaryDto(User user) {

        return UserSummaryDto.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }
}
