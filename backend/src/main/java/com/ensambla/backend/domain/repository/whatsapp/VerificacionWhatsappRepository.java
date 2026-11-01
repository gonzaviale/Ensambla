package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.VerificacionWhatsapp;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface VerificacionWhatsappRepository {

    VerificacionWhatsapp save(VerificacionWhatsapp verificacion);

    Optional<VerificacionWhatsapp> findByUsuarioId(UsuarioId usuarioId);

    void deleteByUsuarioId(UsuarioId usuarioId);

    /** Cuántos códigos se mandaron a ese número (de cualquier cuenta) desde el instante indicado. */
    long contarEnviosRecientesA(String telefono, OffsetDateTime desde);

    /** La verificación en curso del usuario; si no pidió ningún código responde como inexistente. */
    default VerificacionWhatsapp obtenerDelUsuario(UsuarioId usuarioId) {
        return findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Primero pedí un código de verificación"));
    }
}
