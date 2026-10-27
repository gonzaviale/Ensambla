package com.ensambla.backend.domain.repository.proyecto;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.negocio.NegocioId;
import com.ensambla.backend.domain.model.proyecto.Proyecto;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;

import java.util.List;
import java.util.Optional;

public interface ProyectoRepository {

    List<Proyecto> findAllByNegocioId(NegocioId negocioId);

    Optional<Proyecto> findByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId);

    Proyecto save(Proyecto proyecto);

    Proyecto saveConEstructura(Proyecto proyecto);

    void deleteByIdAndNegocioId(ProyectoId proyectoId, NegocioId negocioId);

    default Proyecto obtener(NegocioId negocioId, ProyectoId proyectoId) {
        return findByIdAndNegocioId(proyectoId, negocioId)
                .orElseThrow(() -> new ResourceNotFoundException("Proyecto no encontrado"));
    }
}
