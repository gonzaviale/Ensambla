package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.util.Optional;
import java.util.UUID;

public interface VinculoWhatsappRepository {

    VinculoWhatsapp save(VinculoWhatsapp vinculo);

    Optional<VinculoWhatsapp> findByUsuarioId(UUID usuarioId);

    Optional<VinculoWhatsapp> findByTelefono(String telefono);

    void deleteByUsuarioId(UUID usuarioId);
}
