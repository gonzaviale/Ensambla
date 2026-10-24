package com.ensambla.backend.domain.repository.lote;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.lote.FotoCuaderno;
import com.ensambla.backend.domain.model.lote.FotoId;
import com.ensambla.backend.domain.model.lote.LoteId;
import com.ensambla.backend.domain.model.lote.LoteSubida;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface LoteSubidaRepository {

    /** Guarda el lote y sus fotos; las restricciones de la base fallan acá mismo, no al confirmar la transacción. */
    LoteSubida saveConFotos(LoteSubida lote);

    /**
     * Pasa el lote de LISTO a CONFIRMADO en una sola sentencia condicional. Devuelve false si el lote no es del negocio
     * o ya no estaba LISTO (por ejemplo, otra confirmación se adelantó), así dos confirmaciones no se pisan.
     */
    boolean confirmar(LoteId loteId, NegocioId negocioId, BloqueId bloqueDestinoId, OffsetDateTime ahora);

    /** La foto sólo si es del negocio; una foto ajena se comporta como inexistente. */
    Optional<FotoCuaderno> findFotoByIdAndNegocioId(FotoId fotoId, NegocioId negocioId);

    /** El lote con sus fotos ordenadas por posición, sólo si es del negocio. */
    Optional<LoteSubida> findByIdAndNegocioId(LoteId loteId, NegocioId negocioId);
}
