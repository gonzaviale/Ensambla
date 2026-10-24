package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanes;
import com.ensambla.backend.domain.useCase.plan.consultarPlanes.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.iniciarPrueba.IniciarPrueba;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.PlanPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/planes")
@Tag(name = "Planes")
public class PlanController {

    private final IniciarPrueba iniciarPrueba;

    private final PlanPresentationMapper planPresentationMapper;

    private final ConsultarPlanes consultarPlanes;

    @GetMapping
    @Operation(summary = "Consultar planes, suscripción y uso del negocio de la sesión")
    public ResponseEntity<PlanesResponse> consultar(Authentication authentication) {
        ConsultarPlanesResponse response = consultarPlanes.execute(
                UUID.fromString(authentication.getName()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(planPresentationMapper.toResponse(response));
    }

    @PostMapping("/prueba")
    @Operation(summary = "Iniciar la única prueba premium del negocio de la sesión")
    public ResponseEntity<PruebaResponse> iniciarPrueba(Authentication authentication) {
        PruebaPremiumResponse response = iniciarPrueba.execute(
                planPresentationMapper.toRequest(UUID.fromString(authentication.getName())));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(planPresentationMapper.toResponse(response));
    }
}
