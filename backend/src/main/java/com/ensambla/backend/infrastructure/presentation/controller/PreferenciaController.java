package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.preferencia.guardarPreferencias.GuardarPreferencias;
import com.ensambla.backend.domain.useCase.preferencia.obtenerPreferencias.ObtenerPreferencias;
import com.ensambla.backend.infrastructure.presentation.dto.preferencia.PreferenciasResponse;
import com.ensambla.backend.infrastructure.presentation.dto.preferencia.PreferenciasWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.preferencia.PreferenciaPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/preferencias")
@Tag(name = "Preferencias")
@RequiredArgsConstructor
public class PreferenciaController {

    private final ObtenerPreferencias obtenerPreferencias;

    private final GuardarPreferencias guardarPreferencias;

    private final PreferenciaPresentationMapper preferenciaPresentationMapper;

    @GetMapping
    @Operation(summary = "Consultar las preferencias de la cuenta de la sesión")
    public ResponseEntity<PreferenciasResponse> obtener(Authentication authentication) {
        return ResponseEntity.ok(preferenciaPresentationMapper.toResponse(obtenerPreferencias.execute(
                preferenciaPresentationMapper.toObtenerRequest(UsuarioId.de(authentication.getName())))));
    }

    @PutMapping
    @Operation(summary = "Guardar las preferencias de la cuenta de la sesión")
    public ResponseEntity<PreferenciasResponse> guardar(
            Authentication authentication, @Valid @RequestBody PreferenciasWebRequest request) {
        return ResponseEntity.ok(preferenciaPresentationMapper.toResponse(guardarPreferencias.execute(
                preferenciaPresentationMapper.toGuardarRequest(UsuarioId.de(authentication.getName()), request))));
    }
}
