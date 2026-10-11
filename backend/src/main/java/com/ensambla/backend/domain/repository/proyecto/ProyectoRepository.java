package com.ensambla.backend.domain.repository.proyecto;

import com.ensambla.backend.domain.model.proyecto.Proyecto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProyectoRepository {

    List<Proyecto> findAllByNegocioId(UUID negocioId);

    Optional<Proyecto> findByIdAndNegocioId(UUID proyectoId, UUID negocioId);

    Proyecto save(Proyecto proyecto);

    Proyecto saveConEstructura(Proyecto proyecto);

    void deleteByIdAndNegocioId(UUID proyectoId, UUID negocioId);
}

