package vn.iotstar.models;

import jakarta.validation.constraints.*;

public record RegisterUserModel(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(min = 6, max = 72) String password,
        @NotBlank @Size(max = 50) String fullName) {}
