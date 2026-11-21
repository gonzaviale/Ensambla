package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.listarSemanas.ListarSemanas;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.SemanasObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.ResumenSemanalPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** El cierre del domingo de un objetivo: se ve al entrar, nunca llega como aviso. */
@RestController
@RequestMapping("/api/objetivos/{objetivoId}/semanas")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class ResumenSemanalController {

    private final ListarSemanas listarSemanas;

    private final ResumenSemanalPresentationMapper resumenSemanalPresentationMapper;

    @GetMapping
    @Operation(summary = "Los resúmenes del domingo de un objetivo, con los reajustes de cada semana")
    public ResponseEntity<SemanasObjetivoResponse> listar(
            Authentication authentication, @PathVariable ObjetivoId objetivoId) {
        return ResponseEntity.ok(resumenSemanalPresentationMapper.toResponse(listarSemanas.execute(
                resumenSemanalPresentationMapper.toRequest(UsuarioId.de(authentication.getName()), objetivoId))));
    }
}
