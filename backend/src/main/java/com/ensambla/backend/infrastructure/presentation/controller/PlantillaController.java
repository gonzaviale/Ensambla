package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.plantilla.ListarPlantillasUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.plantilla.PlantillaResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.plantilla.PlantillaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/plantillas")
@Tag(name = "Plantillas")
@RequiredArgsConstructor
public class PlantillaController {

    private final ListarPlantillasUseCase listarPlantillasUseCase;
    private final PlantillaPresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Listar las plantillas activas para crear un proyecto")
    public ResponseEntity<List<PlantillaResponse>> listar() {
        return ResponseEntity.ok(presentationMapper.toResponse(listarPlantillasUseCase.execute()));
    }
}
