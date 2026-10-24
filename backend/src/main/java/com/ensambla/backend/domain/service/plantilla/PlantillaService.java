package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaId;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PlantillaService {

    private final PlantillaRepository plantillaRepository;

    public PlantillaProyecto obtenerParaProyecto(PlantillaId plantillaId) {
        return plantillaRepository.findParaProyectoByIdAndActivaTrue(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
    }
}
