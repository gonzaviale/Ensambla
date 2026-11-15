package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;

import java.util.List;

public interface RespuestaMiniObjetivoRepository {

    /** Guarda la respuesta, en el acto: un mini-objetivo tiene una sola. */
    RespuestaMiniObjetivo save(RespuestaMiniObjetivo respuesta);

    boolean existsByMiniObjetivoId(MiniObjetivoId miniObjetivoId);

    /** Las respuestas de todos los mini-objetivos del objetivo del negocio. Nunca las de otro negocio. */
    List<RespuestaMiniObjetivo> findAllByObjetivoIdAndNegocioId(ObjetivoId objetivoId, NegocioId negocioId);
}
