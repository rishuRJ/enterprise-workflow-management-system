package com.rishu.workflow.dto;

import com.rishu.workflow.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;

    @NotNull(message = "Role is required")
    private Role role;
}
