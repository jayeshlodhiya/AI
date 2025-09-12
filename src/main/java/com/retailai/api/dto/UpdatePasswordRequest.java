package com.retailai.api.dto;



import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank String currentPassword,
        @NotBlank @Size(min = 8, message = "New password must be at least 8 characters")
        String newPassword,
        @NotBlank String confirmPassword
) {}
