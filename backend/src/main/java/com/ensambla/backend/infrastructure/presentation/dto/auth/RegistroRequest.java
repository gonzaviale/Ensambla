package com.ensambla.backend.infrastructure.presentation.dto.auth;

import com.ensambla.backend.infrastructure.presentation.validation.MaxBytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank(message = "Ingresá tu nombre") @Size(max = 100) String nombre,
        @NotBlank(message = "Ingresá el nombre de tu negocio") @Size(max = 120) String nombreNegocio,
        @NotBlank(message = "Ingresá tu email") @Email(message = "Ingresá un email válido")
        @Size(max = 254) String email,
        @NotBlank(message = "Ingresá tu contraseña") @Size(min = 8, message = "Usá al menos 8 caracteres")
        @MaxBytes String contrasenia) {
}

