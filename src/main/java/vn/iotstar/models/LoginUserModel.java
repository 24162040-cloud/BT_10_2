package vn.iotstar.models;

import jakarta.validation.constraints.*;

public record LoginUserModel(
        @NotBlank @Email @Size(max = 100) String email,
        @NotBlank @Size(max = 72) String password) {}
