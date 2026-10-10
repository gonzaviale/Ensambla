package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoDesdePlantillaUseCase;
import com.ensambla.backend.domain.useCase.proyecto.CrearProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.EliminarProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ListarProyectosUseCase;
import com.ensambla.backend.domain.useCase.proyecto.ObtenerProyectoUseCase;
import com.ensambla.backend.domain.useCase.proyecto.RenombrarProyectoUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.CrearProyectoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.ProyectoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.proyecto.RenombrarProyectoWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.proyecto.ProyectoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/proyectos")
@Tag(name = "Proyectos")
@RequiredArgsConstructor
public class ProyectoController {

    private final ListarProyectosUseCase listarProyectosUseCase;

    private final ObtenerProyectoUseCase obtenerProyectoUseCase;

    private final CrearProyectoUseCase crearProyectoUseCase;

    private final RenombrarProyectoUseCase renombrarProyectoUseCase;

    private final CrearProyectoDesdePlantillaUseCase crearProyectoDesdePlantillaUseCase;

    private final EliminarProyectoUseCase eliminarProyectoUseCase;

    private final ProyectoPresentationMapper proyectoPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar proyectos del negocio de la sesión")
    public ResponseEntity<List<ProyectoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(proyectoPresentationMapper.toResponse(listarProyectosUseCase.execute(
                proyectoPresentationMapper.toListarRequest(UUID.fromString(authentication.getName())))));
    }

    @GetMapping("/{proyectoId}")
    @Operation(summary = "Consultar un proyecto y sus bloques y campos")
    public ResponseEntity<ProyectoResponse> obtener(Authentication authentication, @PathVariable UUID proyectoId) {
        return ResponseEntity.ok(proyectoPresentationMapper.toResponse(obtenerProyectoUseCase.execute(
                proyectoPresentationMapper.toObtenerRequest(UUID.fromString(authentication.getName()), proyectoId))));
    }

    @PostMapping
    @Operation(summary = "Crear un proyecto en blanco")
    public ResponseEntity<ProyectoResponse> crear(
            Authentication authentication, @Valid @RequestBody CrearProyectoWebRequest request) {
        ProyectoResponse response = proyectoPresentationMapper.toResponse(crearProyectoUseCase.execute(
                proyectoPresentationMapper.toCrearRequest(UUID.fromString(authentication.getName()), request)));
        return ResponseEntity.created(URI.create("/api/proyectos/" + response.id())).body(response);
    }

    @PostMapping("/desde-plantilla/{plantillaId}")
    @Operation(summary = "Copiar una plantilla al negocio de la sesión")
    public ResponseEntity<ProyectoResponse> desdePlantilla(
            Authentication authentication, @PathVariable UUID plantillaId) {
        ProyectoResponse response = proyectoPresentationMapper.toResponse(crearProyectoDesdePlantillaUseCase.execute(
                proyectoPresentationMapper.toDesdePlantillaRequest(
                        UUID.fromString(authentication.getName()), plantillaId)));
        return ResponseEntity.created(URI.create("/api/proyectos/" + response.id())).body(response);
    }

    @PatchMapping("/{proyectoId}")
    @Operation(summary = "Renombrar un proyecto del negocio de la sesión")
    public ResponseEntity<ProyectoResponse> renombrar(
            Authentication authentication,
            @PathVariable UUID proyectoId,
            @Valid @RequestBody RenombrarProyectoWebRequest request) {
        return ResponseEntity.ok(proyectoPresentationMapper.toResponse(renombrarProyectoUseCase.execute(
                proyectoPresentationMapper.toRenombrarRequest(
                        UUID.fromString(authentication.getName()), proyectoId, request))));
    }

    @DeleteMapping("/{proyectoId}")
    @Operation(summary = "Eliminar un proyecto y sus bloques, campos y registros")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable UUID proyectoId) {
        eliminarProyectoUseCase.execute(proyectoPresentationMapper.toEliminarRequest(
                UUID.fromString(authentication.getName()), proyectoId));
        return ResponseEntity.noContent().build();
    }
}

