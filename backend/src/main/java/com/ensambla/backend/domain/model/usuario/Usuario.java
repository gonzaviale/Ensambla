package com.ensambla.backend.domain.model.usuario;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.Locale;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    private UsuarioId id;

    private String nombre;

    private String email;

    private String hashContrasenia;

    private EstadoUsuario estado;

    private OffsetDateTime creadoEn;

    private OffsetDateTime actualizadoEn;

    public boolean estaActivo() {
        return EstadoUsuario.ACTIVO == estado;
    }

    public static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
