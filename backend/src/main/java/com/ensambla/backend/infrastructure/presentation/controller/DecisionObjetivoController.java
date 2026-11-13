package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.listarDecisiones.ListarDecisiones;
import com.ensambla.backend.domain.useCase.objetivo.registrarDecisionPaso.RegistrarDecisionPaso;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecidirPasoWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.DecisionesObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.DecisionObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lo que la persona decidió frente a un objetivo. Los avisos aplicados o descartados se anotan solos al resolverlos
 * (POST /api/avisos/{id}/aplicar y /descartar); acá se anota el paso de un mini-objetivo y se ve el historial por
 * semana.
 */
@RestController
@RequestMapping("/api/objetivos/{objetivoId}")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class DecisionObjetivoController {

    private final RegistrarDecisionPaso registrarDecisionPaso;

    private final ListarDecisiones listarDecisiones;

    private final DecisionObjetivoPresentationMapper decisionObjetivoPresentationMapper;

    @PostMapping("/mini-objetivos/{miniObjetivoId}/decisiones")
    @Operation(summary = "Aceptar («Lo hago») o ignorar («Ahora no») el paso de un mini-objetivo en curso")
    public ResponseEntity<DecisionObjetivoResponse> decidirPaso(
            Authentication authentication, @PathVariable ObjetivoId objetivoId,
            @PathVariable MiniObjetivoId miniObjetivoId, @Valid @RequestBody DecidirPasoWebRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(decisionObjetivoPresentationMapper.toResponse(
                registrarDecisionPaso.execute(decisionObjetivoPresentationMapper.toRegistrarRequest(
                        UsuarioId.de(authentication.getName()), objetivoId, miniObjetivoId, request))));
    }

    @GetMapping("/decisiones")
    @Operation(summary = "Las decisiones de la persona frente a un objetivo, por semana y con su efecto")
    public ResponseEntity<DecisionesObjetivoResponse> listar(
            Authentication authentication, @PathVariable ObjetivoId objetivoId) {
        return ResponseEntity.ok(decisionObjetivoPresentationMapper.toResponse(listarDecisiones.execute(
                decisionObjetivoPresentationMapper.toListarRequest(
                        UsuarioId.de(authentication.getName()), objetivoId))));
    }
}
