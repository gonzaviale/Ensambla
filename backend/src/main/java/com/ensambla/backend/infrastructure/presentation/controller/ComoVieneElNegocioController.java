package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocio;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioRequest;
import com.ensambla.backend.domain.useCase.objetivo.obtenerComoVieneElNegocio.ObtenerComoVieneElNegocioResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/objetivos/negocio")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class ComoVieneElNegocioController {

    private final ObtenerComoVieneElNegocio obtenerComoVieneElNegocio;

    @GetMapping
    @Operation(summary = "Cómo viene tu negocio: lo vendido, la mercadería, los gastos y lo que queda en los últimos 30 días, y las últimas 6 semanas")
    public ResponseEntity<ObtenerComoVieneElNegocioResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok(obtenerComoVieneElNegocio.execute(
                new ObtenerComoVieneElNegocioRequest(UsuarioId.de(authentication.getName()))));
    }
}
