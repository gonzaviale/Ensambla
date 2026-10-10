package com.ensambla.backend.domain.service.plantilla;

import com.ensambla.backend.domain.model.plantilla.Plantilla;
import com.ensambla.backend.domain.repository.plantilla.PlantillaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlantillaService {

    private final PlantillaRepository plantillaRepository;

    public List<Plantilla> listarActivas() {
        return plantillaRepository.findAllActivas();
    }
}
