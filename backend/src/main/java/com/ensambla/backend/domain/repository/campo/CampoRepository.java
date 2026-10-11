package com.ensambla.backend.domain.repository.campo;

import com.ensambla.backend.domain.model.campo.Campo;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CampoRepository {

    /** Campos del bloque en el orden en que se muestran. */
    List<Campo> findByBloqueIdAndNegocioId(UUID bloqueId, UUID negocioId);

    Optional<Campo> findByIdAndNegocioId(UUID id, UUID negocioId);

    Campo save(Campo campo);

    /** Borra sólo el campo; los valores que tenía en los registros quedan en el JSON sin usarse. */
    void deleteByIdAndNegocioId(UUID id, UUID negocioId);
}
