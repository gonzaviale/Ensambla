package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.listarRespuestasPendientes.ListarRespuestasPendientes;
import com.ensambla.backend.domain.useCase.objetivo.responderMiniObjetivo.ResponderMiniObjetivo;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.RespuestaPendienteResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ResponderMiniObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.RespuestaMiniObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Lo que la persona cuenta de sus mini-objetivos manuales: si los hizo y qué pasó. */
@RestController
@RequestMapping("/api/objetivos")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class RespuestaMiniObjetivoController {

    private final ResponderMiniObjetivo responderMiniObjetivo;

    private final ListarRespuestasPendientes listarRespuestasPendientes;

    private final RespuestaMiniObjetivoPresentationMapper respuestaMiniObjetivoPresentationMapper;

    @PostMapping("/{objetivoId}/mini-objetivos/{miniObjetivoId}/respuesta")
    @Operation(summary = "Responder un mini-objetivo manual: si lo hizo y qué pasó (una sola vez)")
    public ResponseEntity<RespuestaMiniObjetivoResponse> responder(
            Authentication authentication, @PathVariable ObjetivoId objetivoId,
            @PathVariable MiniObjetivoId miniObjetivoId, @Valid @RequestBody ResponderMiniObjetivoWebRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(respuestaMiniObjetivoPresentationMapper.toResponse(
                responderMiniObjetivo.execute(respuestaMiniObjetivoPresentationMapper.toResponderRequest(
                        UsuarioId.de(authentication.getName()), objetivoId, miniObjetivoId, request))));
    }

    @GetMapping("/respuestas-pendientes")
    @Operation(summary = "Los mini-objetivos manuales del negocio de la sesión que esperan una respuesta")
    public ResponseEntity<List<RespuestaPendienteResponse>> pendientes(Authentication authentication) {
        return ResponseEntity.ok(respuestaMiniObjetivoPresentationMapper.toResponse(
                listarRespuestasPendientes.execute(respuestaMiniObjetivoPresentationMapper
                        .toListarPendientesRequest(UsuarioId.de(authentication.getName())))));
    }
}
