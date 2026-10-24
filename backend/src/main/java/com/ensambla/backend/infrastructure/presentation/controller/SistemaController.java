package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.sistema.eliminarSistema.EliminarSistema;
import com.ensambla.backend.domain.useCase.sistema.guardarSistema.GuardarSistema;
import com.ensambla.backend.domain.useCase.sistema.listarSistemas.ListarSistemas;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.sistema.SistemaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.sistema.SistemaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sistemas")
@Tag(name = "Sistemas", description = "Mis sistemas: el proyecto completo, con secciones, bloques, columnas y filas")
@RequiredArgsConstructor
public class SistemaController {

    private final ListarSistemas listarSistemas;

    private final GuardarSistema guardarSistema;

    private final EliminarSistema eliminarSistema;

    private final SistemaPresentationMapper sistemaPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar los sistemas del negocio de la sesión")
    public ResponseEntity<List<SistemaWebResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(sistemaPresentationMapper.toResponse(listarSistemas.execute(
                sistemaPresentationMapper.toListarRequest(UUID.fromString(authentication.getName())))));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Crear o reemplazar un sistema entero; se puede repetir sin duplicar nada")
    public ResponseEntity<SistemaWebResponse> guardar(
            Authentication authentication, @PathVariable String id, @Valid @RequestBody SistemaWebRequest request) {
        return ResponseEntity.ok(sistemaPresentationMapper.toResponse(guardarSistema.execute(
                sistemaPresentationMapper.toGuardarRequest(UUID.fromString(authentication.getName()), id, request))));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un sistema; si ya no existe no falla")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable String id) {
        eliminarSistema.execute(
                sistemaPresentationMapper.toEliminarRequest(UUID.fromString(authentication.getName()), id));
        return ResponseEntity.noContent().build();
    }
}
