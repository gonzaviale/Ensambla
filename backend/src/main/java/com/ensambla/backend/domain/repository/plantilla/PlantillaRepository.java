package com.ensambla.backend.domain.repository.plantilla;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlantillaRepository {

    List<Plantilla> findAllActivas();

    Optional<PlantillaProyecto> findParaProyectoByIdAndActivaTrue(UUID plantillaId);
}
