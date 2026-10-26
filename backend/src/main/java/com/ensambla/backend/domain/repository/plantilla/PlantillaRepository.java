package com.ensambla.backend.domain.repository.plantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;

import java.util.List;
import java.util.Optional;

public interface PlantillaRepository {

    List<Plantilla> findAllActivas();

    /** Las plantillas activas con su estructura ya interpretada, para copiarlas a un proyecto. */
    List<PlantillaProyecto> findAllParaProyectoActivas();

    Optional<PlantillaProyecto> findParaProyectoByIdAndActivaTrue(PlantillaId plantillaId);

    default PlantillaProyecto obtenerParaProyecto(PlantillaId plantillaId) {
        return findParaProyectoByIdAndActivaTrue(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
    }
}
