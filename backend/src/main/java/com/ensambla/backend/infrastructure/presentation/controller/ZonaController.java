package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.zona.AnalizarZonaUseCase;
import com.ensambla.backend.domain.useCase.zona.ObtenerZonaUseCase;
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

    private final ObtenerZonaUseCase obtenerZonaUseCase;

    private final AnalizarZonaUseCase analizarZonaUseCase;

    private final ZonaPresentationMapper zonaPresentationMapper;

    @GetMapping("/agregados")
    @Operation(summary = "Precios y ventas agregados de la zona del negocio de la sesión")
    public ResponseEntity<ZonaWebResponse> agregados(Authentication authentication) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(obtenerZonaUseCase.execute(
                zonaPresentationMapper.toObtenerRequest(UUID.fromString(authentication.getName())))));
    }

    @PostMapping("/analisis")
    @Operation(
            summary = "Precios y ventas agregados de otra zona, indicada por un punto del mapa, un barrio o una dirección",
            description = "Con latitud y longitud resuelve la zona precargada que contiene el punto; si no, busca el "
                    + "barrio o la dirección escritos. Si en la zona hay menos de 3 comercios devuelve la lista de "
                    + "productos vacía (no hay datos suficientes). No filtra por rubro. Los productos vienen ordenados "
                    + "por cuántos comercios los venden.")
    public ResponseEntity<ZonaWebResponse> analizar(@Valid @RequestBody AnalizarZonaWebRequest request) {
        return ResponseEntity.ok(zonaPresentationMapper.toResponse(
                analizarZonaUseCase.execute(zonaPresentationMapper.toAnalizarRequest(request))));
    }
}
