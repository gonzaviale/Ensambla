package com.ensambla.backend.domain.repository.lote;

import com.ensambla.backend.domain.model.lote.LoteSubida;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface LoteSubidaRepository {

    /** Guarda el lote y sus fotos; las restricciones de la base fallan acá mismo, no al confirmar la transacción. */
    LoteSubida saveConFotos(LoteSubida lote);

    /**
     * Pasa el lote de LISTO a CONFIRMADO en una sola sentencia condicional. Devuelve false si el lote no es del negocio
     * o ya no estaba LISTO (por ejemplo, otra confirmación se adelantó), así dos confirmaciones no se pisan.
     */
    boolean confirmar(UUID loteId, UUID negocioId, UUID bloqueDestinoId, OffsetDateTime ahora);

    /** El lote con sus fotos ordenadas por posición, sólo si es del negocio. */
    Optional<LoteSubida> findByIdAndNegocioId(UUID loteId, UUID negocioId);
}
