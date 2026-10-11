package com.ensambla.backend.domain.dto.auth;

public record RegistrarUsuarioRequest(String nombre, String nombreNegocio, String email, String contrasenia) {
}

