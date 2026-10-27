package com.ensambla.backend.domain.repository.bloque;

import com.ensambla.backend.domain.exception.CampoInvalidoException;
import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.negocio.NegocioId;

import java.util.Map;
import java.util.Optional;

public interface BloqueRepository {

    Optional<Bloque> findByIdAndNegocioId(BloqueId id, NegocioId negocioId);

    Bloque save(Bloque bloque);

    /** Borra el bloque; la base elimina en cascada sus campos y registros. */
    void deleteByIdAndNegocioId(BloqueId id, NegocioId negocioId);

    default Bloque obtener(BloqueId bloqueId, NegocioId negocioId) {
        return findByIdAndNegocioId(bloqueId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("El bloque no existe"));
    }

    /**
     * La tabla con la que se conecta un campo RELACION tiene que ser del mismo sistema (proyecto) que su bloque; puede
     * ser el propio bloque. Una tabla de otro negocio responde igual que una inexistente.
     */
    default void validarBloqueRelacionado(Bloque bloque, Campo campo) {
        if (!campo.esRelacion()) {
            return;
        }
        boolean delMismoProyecto = findByIdAndNegocioId(campo.getBloqueRelacionadoId(), bloque.getNegocioId())
                .filter(relacionado -> relacionado.getProyectoId().equals(bloque.getProyectoId()))
                .isPresent();
        if (!delMismoProyecto) {
            throw new CampoInvalidoException(Map.of("bloqueRelacionadoId", "Elegí una tabla de este sistema"));
        }
    }
}
