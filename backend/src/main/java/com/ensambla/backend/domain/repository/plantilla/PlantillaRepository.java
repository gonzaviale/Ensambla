package com.ensambla.backend.domain.repository.plantilla;

import com.ensambla.backend.domain.model.plantilla.Plantilla;

import java.util.List;

public interface PlantillaRepository {

    List<Plantilla> findAllActivas();
}
