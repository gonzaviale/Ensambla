package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.util.Optional;

public interface VinculoWhatsappRepository {

    VinculoWhatsapp save(VinculoWhatsapp vinculo);

    Optional<VinculoWhatsapp> findByUsuarioId(UsuarioId usuarioId);

    Optional<VinculoWhatsapp> findByTelefono(String telefono);

    void deleteByUsuarioId(UsuarioId usuarioId);

    /** El número vinculado del usuario; si no tiene ninguno responde como inexistente. */
    default VinculoWhatsapp obtenerDelUsuario(UsuarioId usuarioId) {
        return findByUsuarioId(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No hay un número de WhatsApp vinculado"));
    }
}
