package com.ensambla.backend.domain.repository.whatsapp;

import com.ensambla.backend.domain.model.whatsapp.CodigoVinculacion;

import java.util.Optional;
import java.util.UUID;

public interface CodigoVinculacionRepository {

    CodigoVinculacion save(CodigoVinculacion codigo);

    Optional<CodigoVinculacion> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    /** Descarta los códigos que el usuario pidió y no llegó a usar. */
    void deleteSinUsarByUsuarioId(UUID usuarioId);
}
