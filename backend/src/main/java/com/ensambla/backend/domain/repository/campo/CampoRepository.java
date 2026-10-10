package com.ensambla.backend.domain.repository.campo;

import com.ensambla.backend.domain.model.campo.Campo;

import java.util.List;
import java.util.UUID;

public interface CampoRepository {

    /** Campos del bloque en el orden en que se muestran. */
    List<Campo> findByBloqueIdAndNegocioId(UUID bloqueId, UUID negocioId);
}
