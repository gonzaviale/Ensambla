package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.Objetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

import java.util.List;
import java.util.Optional;

public interface ObjetivoRepository {

    /** Del más viejo al más nuevo. */
    List<Objetivo> findAllByNegocioId(NegocioId negocioId);

    /** Los negocios que tienen objetivos, ordenados, de a {@code tamano} por {@code pagina} (desde 0). */
    List<NegocioId> findNegocioIdsConObjetivos(int pagina, int tamano);

    Optional<Objetivo> findByIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId);

    Objetivo save(Objetivo objetivo);

    void deleteByIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId);

    default Objetivo obtener(NegocioId negocioId, ObjetivoId objetivoId) {
        return findByIdAndNegocioId(objetivoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Objetivo no encontrado"));
    }
}
