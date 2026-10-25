package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.dto.plan.PruebaPremiumResponse;
import com.ensambla.backend.domain.useCase.plan.ListarCatalogoPlanesUseCase;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.CatalogoPlanesPresentationMapper;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse.PlanResponse;
import com.ensambla.backend.domain.dto.plan.ConsultarPlanesResponse;
import com.ensambla.backend.domain.useCase.plan.ConsultarPlanesUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PlanesResponse;
import com.ensambla.backend.domain.useCase.plan.IniciarPruebaUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.plan.PruebaResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.plan.PlanPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/planes")
@Tag(name = "Planes")
public class PlanController {

    private final IniciarPruebaUseCase iniciarPruebaUseCase;

    private final PlanPresentationMapper planPresentationMapper;

    private final ConsultarPlanesUseCase consultarPlanesUseCase;

    private final ListarCatalogoPlanesUseCase listarCatalogoPlanesUseCase;

    private final CatalogoPlanesPresentationMapper catalogoPlanesPresentationMapper;

    @GetMapping("/catalogo")
    @Operation(summary = "Consultar el catálogo público sin datos de negocios ni suscripciones")
    public ResponseEntity<List<PlanResponse>> listarCatalogo() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(catalogoPlanesPresentationMapper.toResponse(listarCatalogoPlanesUseCase.execute()));
    }

    @GetMapping
    @Operation(summary = "Consultar planes, suscripción y uso del negocio de la sesión")
    public ResponseEntity<PlanesResponse> consultar(Authentication authentication) {
        ConsultarPlanesResponse response = consultarPlanesUseCase.execute(
                UUID.fromString(authentication.getName()));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(planPresentationMapper.toResponse(response));
    }

    @PostMapping("/prueba")
    @Operation(summary = "Iniciar la única prueba premium del negocio de la sesión")
    public ResponseEntity<PruebaResponse> iniciarPrueba(Authentication authentication) {
        PruebaPremiumResponse response = iniciarPruebaUseCase.execute(
                planPresentationMapper.toRequest(UUID.fromString(authentication.getName())));
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(planPresentationMapper.toResponse(response));
    }
}
