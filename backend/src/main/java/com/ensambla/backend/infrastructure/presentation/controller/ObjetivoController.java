package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerPasos.ObtenerPasos;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PasoObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/objetivos")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class ObjetivoController {

    private final ObtenerPasos obtenerPasos;

    private final ObjetivoPresentationMapper objetivoPresentationMapper;

    @GetMapping("/pasos")
    @Operation(summary = "Productos reales del negocio elegidos para los pasos concretos de sus objetivos")
    public ResponseEntity<List<PasoObjetivoWebResponse>> pasos(Authentication authentication) {
        return ResponseEntity.ok(objetivoPresentationMapper.toResponse(obtenerPasos.execute(
                objetivoPresentationMapper.toObtenerPasosRequest(
                        UsuarioId.de(authentication.getName())))));
    }
}
