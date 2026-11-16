package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.cartel.armarOpcionesDeCartel.ArmarOpcionesDeCartel;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelResponse;
import com.ensambla.backend.infrastructure.presentation.dto.cartel.OpcionesDeCartelWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.cartel.CartelPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carteles")
@Tag(name = "Carteles")
@RequiredArgsConstructor
public class CartelController {

    private final ArmarOpcionesDeCartel armarOpcionesDeCartel;

    private final CartelPresentationMapper cartelPresentationMapper;

    @PostMapping("/opciones")
    @Operation(summary = "Armar las 3 opciones de cartel con la marca del negocio de la sesión")
    public ResponseEntity<OpcionesDeCartelResponse> armarOpciones(
            Authentication authentication, @Valid @RequestBody OpcionesDeCartelWebRequest request) {
        return ResponseEntity.ok(cartelPresentationMapper.toResponse(armarOpcionesDeCartel.execute(
                cartelPresentationMapper.toArmarRequest(UsuarioId.de(authentication.getName()), request))));
    }
}
