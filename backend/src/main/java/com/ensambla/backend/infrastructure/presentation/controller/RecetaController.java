package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.receta.GuardarRecetaUseCase;
import com.ensambla.backend.domain.useCase.receta.ObtenerRecetaUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.receta.RecetaRequest;
import com.ensambla.backend.infrastructure.presentation.dto.receta.RecetaResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.receta.RecetaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/registros/{registroId}/receta")
@Tag(name = "Recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final ObtenerRecetaUseCase obtenerRecetaUseCase;

    private final GuardarRecetaUseCase guardarRecetaUseCase;

    private final RecetaPresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Ver la receta de un producto con su costo calculado con el precio actual de los insumos")
    public ResponseEntity<RecetaResponse> obtener(Authentication authentication, @PathVariable UUID registroId) {
        RecetaResponse response = presentationMapper.toResponse(obtenerRecetaUseCase.execute(
                presentationMapper.toObtenerRequest(usuarioDe(authentication), registroId)));
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Guardar la receta de un producto (reemplaza la anterior; sin líneas la quita)")
    public ResponseEntity<RecetaResponse> guardar(Authentication authentication,
                                                  @PathVariable UUID registroId,
                                                  @RequestBody RecetaRequest request) {
        RecetaResponse response = presentationMapper.toResponse(guardarRecetaUseCase.execute(
                presentationMapper.toGuardarRequest(usuarioDe(authentication), registroId, request)));
        return ResponseEntity.ok(response);
    }

    private static UUID usuarioDe(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
