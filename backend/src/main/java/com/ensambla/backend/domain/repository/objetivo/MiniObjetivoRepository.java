package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

import java.util.List;
import java.util.Optional;

public interface MiniObjetivoRepository {

    /** Del más viejo al más nuevo y, a igualdad, por posición. Nunca los de otro negocio. */
    List<MiniObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId);

    Optional<MiniObjetivo> findByIdAndObjetivoIdAndNegocioId(
            MiniObjetivoId miniObjetivoId, ObjetivoId objetivoId, NegocioId negocioId);

    /** Guarda y escribe en el acto: el orden de las escrituras importa (un lugar sólo lo ocupa uno en curso). */
    MiniObjetivo save(MiniObjetivo miniObjetivo);

    /** El mini-objetivo del objetivo del negocio; uno ajeno responde como inexistente. */
    default MiniObjetivo obtener(NegocioId negocioId, ObjetivoId objetivoId, MiniObjetivoId miniObjetivoId) {
        return findByIdAndObjetivoIdAndNegocioId(miniObjetivoId, objetivoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Mini-objetivo no encontrado"));
    }
}
