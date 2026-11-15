package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.guardarObjetivoConSami.GuardarObjetivoConSami;
import com.ensambla.backend.domain.useCase.objetivo.proponerObjetivoConSami.ProponerObjetivoConSami;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.GuardarObjetivoConSamiWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.PropuestaObjetivoWebResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ProponerObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.SamiObjetivoPresentationMapper;
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

import java.net.URI;

/** Armar un objetivo hablando con Sami: la propuesta (sin guardar nada) y, después, guardar lo que se aceptó. */
@RestController
@RequestMapping("/api/objetivos/sami")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class SamiObjetivoController {

    private final ProponerObjetivoConSami proponerObjetivoConSami;

    private final GuardarObjetivoConSami guardarObjetivoConSami;

    private final SamiObjetivoPresentationMapper samiObjetivoPresentationMapper;

    @PostMapping("/propuesta")
    @Operation(summary = "Contarle un objetivo a Sami: contesta con los datos del negocio y propone hasta 3 "
            + "mini-objetivos, medidos o manuales, sin guardar nada")
    public ResponseEntity<PropuestaObjetivoWebResponse> proponer(
            Authentication authentication, @Valid @RequestBody ProponerObjetivoWebRequest request) {
        return ResponseEntity.ok(samiObjetivoPresentationMapper.toResponse(proponerObjetivoConSami.execute(
                samiObjetivoPresentationMapper.toProponerRequest(UsuarioId.de(authentication.getName()), request))));
    }

    @PostMapping
    @Operation(summary = "Guardar el objetivo armado con Sami con los mini-objetivos que la persona aceptó")
    public ResponseEntity<ObjetivoResponse> guardar(
            Authentication authentication, @Valid @RequestBody GuardarObjetivoConSamiWebRequest request) {
        ObjetivoResponse response = samiObjetivoPresentationMapper.toResponse(guardarObjetivoConSami.execute(
                samiObjetivoPresentationMapper.toGuardarRequest(UsuarioId.de(authentication.getName()), request)));
        return ResponseEntity.created(URI.create("/api/objetivos/" + response.id())).body(response);
    }
}
