package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.negocio.ActualizarNegocioUseCase;
import com.ensambla.backend.domain.useCase.negocio.ObtenerConfiguracionNegocioUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ActualizarNegocioWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.negocio.ConfiguracionNegocioWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.negocio.NegocioPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/business")
@Tag(name = "Configuración del negocio")
@RequiredArgsConstructor
public class NegocioController {

    private final ObtenerConfiguracionNegocioUseCase obtenerConfiguracionNegocioUseCase;
    private final ActualizarNegocioUseCase actualizarNegocioUseCase;
    private final NegocioPresentationMapper negocioPresentationMapper;

    @GetMapping
    @Operation(summary = "Consultar la configuración del negocio del dueño autenticado")
    public ResponseEntity<ConfiguracionNegocioWebResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(negocioPresentationMapper.toResponse(
                obtenerConfiguracionNegocioUseCase.execute(negocioPresentationMapper.toObtenerRequest(
                        UUID.fromString(authentication.getName())))));
    }

    @PatchMapping
    @Operation(summary = "Actualizar únicamente la configuración del negocio del dueño autenticado")
    public ResponseEntity<ConfiguracionNegocioWebResponse> actualizar(Authentication authentication,
            @Valid @RequestBody ActualizarNegocioWebRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(negocioPresentationMapper.toResponse(
                actualizarNegocioUseCase.execute(negocioPresentationMapper.toActualizarRequest(
                        UUID.fromString(authentication.getName()), request))));
    }
}
