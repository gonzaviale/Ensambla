package com.ensambla.backend.domain.useCase.auth.registrarUsuario;

public record RegistrarUsuarioRequest(String nombre, String nombreNegocio, String email, String contrasenia) {
}

