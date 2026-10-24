package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class PlantillaService {

    private final PlantillaRepository plantillaRepository;

    public List<PlantillaProyecto> listarParaProyecto() {
        return plantillaRepository.findAllParaProyectoActivas();
    }

    public PlantillaProyecto obtenerParaProyecto(UUID plantillaId) {
        return plantillaRepository.findParaProyectoByIdAndActivaTrue(plantillaId)
                .orElseThrow(() -> new ResourceNotFoundException("Plantilla no encontrada"));
    }
}
