package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.DecisionObjetivo;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;

import java.util.List;

public interface DecisionObjetivoRepository {

    DecisionObjetivo save(DecisionObjetivo decision);

    /** Las decisiones del objetivo, de la más nueva a la más vieja. Nunca las de otro negocio. */
    List<DecisionObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId);
}
