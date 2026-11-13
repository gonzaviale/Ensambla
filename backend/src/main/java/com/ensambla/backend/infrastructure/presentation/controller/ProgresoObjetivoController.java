package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerProgreso.ObtenerProgreso;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProgresoObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ProgresoObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Lo que el monitoreo silencioso midió de un objetivo: se ve al entrar, nunca llega como aviso. */
@RestController
@RequestMapping("/api/objetivos/{objetivoId}/progreso")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class ProgresoObjetivoController {

    private final ObtenerProgreso obtenerProgreso;

    private final ProgresoObjetivoPresentationMapper progresoObjetivoPresentationMapper;

    @GetMapping
    @Operation(summary = "El avance semanal de un objetivo y de sus mini-objetivos")
    public ResponseEntity<ProgresoObjetivoResponse> obtener(
            Authentication authentication, @PathVariable ObjetivoId objetivoId) {
        return ResponseEntity.ok(progresoObjetivoPresentationMapper.toResponse(obtenerProgreso.execute(
                progresoObjetivoPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), objetivoId))));
    }
}
