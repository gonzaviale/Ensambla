package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.model.whatsapp.TelefonoWhatsapp;
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

    /** El número tiene que estar libre o ser del mismo usuario; si es de otra cuenta responde con un conflicto. */
    default void exigirLibre(TelefonoWhatsapp telefono, UsuarioId usuarioId) {
        boolean ocupadoPorOtro = findByTelefono(telefono.valor())
                .filter(vinculo -> !vinculo.getUsuarioId().equals(usuarioId))
                .isPresent();
        if (ocupadoPorOtro) {
            throw new DuplicateResourceException("Ese número de WhatsApp ya está vinculado a otra cuenta");
        }
    }
}
