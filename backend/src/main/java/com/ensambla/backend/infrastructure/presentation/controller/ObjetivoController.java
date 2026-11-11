package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.crearObjetivo.CrearObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.eliminarObjetivo.EliminarObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarObjetivos.ListarObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CrearObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/objetivos")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class ObjetivoController {

    private final ListarObjetivos listarObjetivos;

    private final CrearObjetivo crearObjetivo;

    private final EliminarObjetivo eliminarObjetivo;

    private final ObtenerPasos obtenerPasos;

    private final ObjetivoPresentationMapper objetivoPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar los objetivos del negocio de la sesión")
    public ResponseEntity<List<ObjetivoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(objetivoPresentationMapper.toResponse(listarObjetivos.execute(
                objetivoPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName())))));
    }

    @PostMapping
    @Operation(summary = "Guardar un objetivo en el negocio de la sesión")
    public ResponseEntity<ObjetivoResponse> crear(
            Authentication authentication, @Valid @RequestBody CrearObjetivoWebRequest request) {
        ObjetivoResponse response = objetivoPresentationMapper.toResponse(crearObjetivo.execute(
                objetivoPresentationMapper.toCrearRequest(UsuarioId.de(authentication.getName()), request)));
        return ResponseEntity.created(URI.create("/api/objetivos/" + response.id())).body(response);
    }

    @DeleteMapping("/{objetivoId}")
    @Operation(summary = "Eliminar un objetivo del negocio de la sesión")
    public ResponseEntity<Void> eliminar(Authentication authentication, @PathVariable ObjetivoId objetivoId) {
        eliminarObjetivo.execute(objetivoPresentationMapper.toEliminarRequest(
                UsuarioId.de(authentication.getName()), objetivoId));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/pasos")
    @Operation(summary = "Productos reales del negocio elegidos para los pasos concretos de sus objetivos")
    public ResponseEntity<List<PasoObjetivoWebResponse>> pasos(Authentication authentication) {
        return ResponseEntity.ok(objetivoPresentationMapper.toResponse(obtenerPasos.execute(
                objetivoPresentationMapper.toObtenerPasosRequest(
                        UsuarioId.de(authentication.getName())))));
    }
}
