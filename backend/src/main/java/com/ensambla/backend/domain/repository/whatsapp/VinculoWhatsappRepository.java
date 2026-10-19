package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.VinculoWhatsapp;

import java.util.Optional;
import java.util.UUID;

public interface VinculoWhatsappRepository {

    /** Guarda y escribe en la base enseguida, así las restricciones de unicidad fallan acá y en orden. */
    VinculoWhatsapp save(VinculoWhatsapp vinculo);

    Optional<VinculoWhatsapp> findByNegocioId(UUID negocioId);

    Optional<VinculoWhatsapp> findByCodigo(String codigo);

    /** El vínculo del número; sólo existe si el número está vinculado. */
    Optional<VinculoWhatsapp> findByTelefono(String telefono);

    boolean existsByCodigo(String codigo);
}
