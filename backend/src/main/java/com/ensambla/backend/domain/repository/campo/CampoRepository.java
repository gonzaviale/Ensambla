package com.ensambla.backend.domain.repository.campo;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.List;
import java.util.Optional;

public interface CampoRepository {

    /** Campos del bloque en el orden en que se muestran. */
    List<Campo> findByBloqueIdAndNegocioId(BloqueId bloqueId, NegocioId negocioId);

    Optional<Campo> findByIdAndNegocioId(CampoId id, NegocioId negocioId);

    /**
     * Campos RELACION de otros bloques del negocio que apuntan a {@code bloqueId} (sin contar los del propio bloque),
     * en orden de bloque y posición.
     */
    List<Campo> findRelacionesHacia(BloqueId bloqueId, NegocioId negocioId);

    Campo save(Campo campo);

    /** Borra sólo el campo; los valores que tenía en los registros quedan en el JSON sin usarse. */
    void deleteByIdAndNegocioId(CampoId id, NegocioId negocioId);

    default Campo obtener(CampoId campoId, NegocioId negocioId) {
        return findByIdAndNegocioId(campoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El campo no existe"));
    }
}
