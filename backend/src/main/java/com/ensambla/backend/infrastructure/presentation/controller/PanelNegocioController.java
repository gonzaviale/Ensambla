package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.reporte.obtenerPanelNegocio.ObtenerPanelNegocio;
import com.ensambla.backend.infrastructure.presentation.dto.reporte.PanelNegocioResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.reporte.PanelNegocioPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/negocio/panel")
@Tag(name = "Reportes")
@RequiredArgsConstructor
public class PanelNegocioController {

    private final ObtenerPanelNegocio obtenerPanelNegocio;

    private final PanelNegocioPresentationMapper presentationMapper;

    @GetMapping
    @Operation(summary = "Métricas y flujo de caja por semana de un sistema, para la home; los días son los últimos, 30 por defecto")
    public ResponseEntity<PanelNegocioResponse> obtener(
            Authentication authentication,
            @RequestParam ProyectoId proyectoId,
            @RequestParam(required = false) Integer dias) {
        return ResponseEntity.ok(presentationMapper.toResponse(obtenerPanelNegocio.execute(
                presentationMapper.toRequest(UsuarioId.de(authentication.getName()), proyectoId, dias))));
    }
}
