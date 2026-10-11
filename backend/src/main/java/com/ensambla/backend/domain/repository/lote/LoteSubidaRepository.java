package com.ensambla.backend.domain.repository.lote;

import com.ensambla.backend.domain.model.lote.LoteSubida;

import java.util.Optional;
import java.util.UUID;

public interface LoteSubidaRepository {

    /** Guarda el lote y sus fotos; las restricciones de la base fallan acá mismo, no al confirmar la transacción. */
    LoteSubida saveConFotos(LoteSubida lote);

    /** El lote con sus fotos ordenadas por posición, sólo si es del negocio. */
    Optional<LoteSubida> findByIdAndNegocioId(UUID loteId, UUID negocioId);
}
