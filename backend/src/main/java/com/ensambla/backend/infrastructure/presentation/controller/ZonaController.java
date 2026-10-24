package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.zona.analizarZona.AnalizarZona;
import com.ensambla.backend.domain.useCase.zona.obtenerZona.ObtenerZona;
import com.ensambla.backend.infrastructure.presentation.dto.zona.AnalizarZonaWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.zona.ZonaWebResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.zona.ZonaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/zona")
@Tag(name = "Zona", description = "Datos agregados y anónimos de los comercios de una zona")
@RequiredArgsConstructor
public class ZonaController {

    private final ObtenerZona obtenerZona;

    private final AnalizarZona analizarZona;

    private final ZonaPresentationMapper zonaPresentationMapper;

    @GetMapping("/agregados")
    @Operation(summary = "Precios y ventas agregados de la zona del negocio de la sesión")
    public ResponseEntity<ZonaWebResponse> agregados(Authentication authentication) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(obtenerZona.execute(
                zonaPresentationMapper.toObtenerRequest(UUID.fromString(authentication.getName())))));
    }

    @PostMapping("/analisis")
    @Operation(summary = "Precios y ventas agregados de otra zona, indicada por barrio o dirección")
    public ResponseEntity<ZonaWebResponse> analizar(Authentication authentication,
                                                    @Valid @RequestBody AnalizarZonaWebRequest request) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(
                analizarZona.execute(zonaPresentationMapper.toAnalizarRequest(
                        request, UUID.fromString(authentication.getName())))));
    }
}
