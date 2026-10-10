package com.ensambla.backend.infrastructure.presentation.dto.auth;

import com.ensambla.backend.infrastructure.presentation.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "Ingresá tu email") @Email(message = "Ingresá un email válido")
        @Size(max = 254) String email,
        @NotBlank(message = "Ingresá tu contraseña") @MaxBytes String contrasenia) {
}

