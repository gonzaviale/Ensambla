package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.receta.guardarReceta.GuardarReceta;
import com.ensambla.backend.domain.useCase.receta.obtenerReceta.ObtenerReceta;
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

@RestController
@RequestMapping("/api/registros/{registroId}/receta")
@Tag(name = "Recetas")
@RequiredArgsConstructor
public class RecetaController {

    private final ObtenerReceta obtenerReceta;

    private final GuardarReceta guardarReceta;

    private final RecetaPresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Ver la receta de un producto con su costo calculado con el precio actual de los insumos")
    public ResponseEntity<RecetaResponse> obtener(Authentication authentication, @PathVariable RegistroId registroId) {
        RecetaResponse response = presentationMapper.toResponse(obtenerReceta.execute(
                presentationMapper.toObtenerRequest(usuarioDe(authentication), registroId)));
        return ResponseEntity.ok(response);
    }

    @PutMapping
    @Operation(summary = "Guardar la receta de un producto (reemplaza la anterior; sin líneas la quita)")
    public ResponseEntity<RecetaResponse> guardar(Authentication authentication,
                                                  @PathVariable RegistroId registroId,
                                                  @RequestBody RecetaRequest request) {
        RecetaResponse response = presentationMapper.toResponse(guardarReceta.execute(
                presentationMapper.toGuardarRequest(usuarioDe(authentication), registroId, request)));
        return ResponseEntity.ok(response);
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
