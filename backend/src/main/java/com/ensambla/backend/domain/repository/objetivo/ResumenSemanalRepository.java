package com.ensambla.backend.domain.repository.objetivo;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ResumenSemanal;

import java.util.List;

public interface ResumenSemanalRepository {

    /**
     * Guarda el resumen con sus reajustes, en el acto.
     *
     * @throws DuplicateResourceException si esa semana del objetivo ya estaba cerrada (otra corrida llegó antes)
     */
    ResumenSemanal save(ResumenSemanal resumen);

    /**
     * Los resúmenes del objetivo con sus reajustes, del más nuevo al más viejo. No filtra por negocio: quien llama ya
     * comprobó que el objetivo es del negocio de la sesión.
     */
    List<ResumenSemanal> findAllByObjetivoId(ObjetivoId objetivoId);
}
