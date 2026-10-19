package com.rishu.workflow.mapper;

import com.rishu.workflow.dto.RequestResponseDto;
import com.rishu.workflow.entity.Request;
import com.rishu.workflow.entity.User;
import com.rishu.workflow.enums.RequestStatus;
import com.rishu.workflow.enums.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RequestMapperTest {

    private final RequestMapper requestMapper = new RequestMapper(new UserMapper());

    @Test
    void toDto_copiesRequestFieldsAndBothUsers(){
        Request request = Request.builder()
                .id(10L)
                .title("title")
                .description("description")
                .status(RequestStatus.PENDING)
                .employee(User.builder().id(1L).role(Role.ROLE_EMPLOYEE).email("employee@test.com").build())
                .manager(User.builder().id(2L).role(Role.ROLE_MANAGER).email("manager@test.com").build())
                .build();

        RequestResponseDto responseDto= requestMapper.toDto(request);

        assertThat(responseDto.getId()).isEqualTo(10L);
        assertThat(responseDto.getDescription()).isEqualTo("description");
        assertThat(responseDto.getTitle()).isEqualTo("title");
        assertThat(responseDto.getStatus()).isEqualTo(RequestStatus.PENDING);
        assertThat(responseDto.getEmployee().getEmail()).isEqualTo("employee@test.com");
        assertThat(responseDto.getManager().getEmail()).isEqualTo("manager@test.com");
    }

}
