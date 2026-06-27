package com.rishu.workflow.dto;


import com.rishu.workflow.enums.Role;
import lombok.Data;

@Data
public class UserResponseDto {
    private int id;
    private String name;
    private String email;
    private Role role;
}
