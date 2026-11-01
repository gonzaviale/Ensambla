package com.ensambla.backend.support;

import com.ensambla.backend.domain.model.usuario.EstadoUsuario;
import com.ensambla.backend.domain.model.usuario.Usuario;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.infrastructure.repository.entity.UsuarioEntity;

import java.time.OffsetDateTime;

public final class UsuarioTestData {

    public static final UsuarioId USUARIO_ID = UsuarioId.de("b9000000-0000-4000-8000-000000000001");
    public static final String NOMBRE = "Persona";
    public static final String EMAIL = "persona@example.com";
    public static final String CONTRASENIA = "Una contraseña segura";
    public static final String HASH = "{bcrypt}hash-de-prueba";
    public static final OffsetDateTime FECHA = OffsetDateTime.parse("2026-10-09T12:00:00Z");

    private UsuarioTestData() {
    }

    public static Usuario usuario() {
        return Usuario.builder()
                .id(USUARIO_ID)
                .nombre(NOMBRE)
                .email(EMAIL)
                .hashContrasenia(HASH)
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(FECHA)
                .actualizadoEn(FECHA)
                .build();
    }

    public static UsuarioEntity entity() {
        return UsuarioEntity.builder()
                .id(USUARIO_ID.valor())
                .nombre(NOMBRE)
                .email(EMAIL)
                .hashContrasenia(HASH)
                .estado(EstadoUsuario.ACTIVO)
                .creadoEn(FECHA)
                .actualizadoEn(FECHA)
                .build();
    }
}
