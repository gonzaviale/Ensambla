package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.RespuestaMiniObjetivo;

public interface RespuestaMiniObjetivoRepository {

    /** Guarda la respuesta, en el acto: un mini-objetivo tiene una sola. */
    RespuestaMiniObjetivo save(RespuestaMiniObjetivo respuesta);

    boolean existsByMiniObjetivoId(MiniObjetivoId miniObjetivoId);
}
