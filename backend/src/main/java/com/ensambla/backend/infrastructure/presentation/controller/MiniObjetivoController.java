package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.objetivo.MiniObjetivoId;
import com.ensambla.backend.domain.model.objetivo.ObjetivoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.objetivo.abandonarMiniObjetivo.AbandonarMiniObjetivo;
import com.ensambla.backend.domain.useCase.objetivo.listarCandidatos.ListarCandidatos;
import com.ensambla.backend.domain.useCase.objetivo.listarMiniObjetivos.ListarMiniObjetivos;
import com.ensambla.backend.domain.useCase.objetivo.reemplazarMiniObjetivo.ReemplazarMiniObjetivo;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.CandidatoMiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.MiniObjetivosResponse;
import com.ensambla.backend.infrastructure.presentation.dto.objetivo.ReemplazarMiniObjetivoWebRequest;
import com.ensambla.backend.infrastructure.presentation.mapper.objetivo.MiniObjetivoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Los mini-objetivos de un objetivo: se proponen al definirlo (POST /api/objetivos) y acá se ven y se cambian. */
@RestController
@RequestMapping("/api/objetivos/{objetivoId}/mini-objetivos")
@Tag(name = "Objetivos")
@RequiredArgsConstructor
public class MiniObjetivoController {

    private final ListarMiniObjetivos listarMiniObjetivos;

    private final ListarCandidatos listarCandidatos;

    private final ReemplazarMiniObjetivo reemplazarMiniObjetivo;

    private final AbandonarMiniObjetivo abandonarMiniObjetivo;

    private final MiniObjetivoPresentationMapper miniObjetivoPresentationMapper;

    @GetMapping
    @Operation(summary = "Los mini-objetivos de un objetivo del negocio de la sesión")
    public ResponseEntity<MiniObjetivosResponse> listar(
            Authentication authentication, @PathVariable ObjetivoId objetivoId) {
        return ResponseEntity.ok(miniObjetivoPresentationMapper.toResponse(listarMiniObjetivos.execute(
                miniObjetivoPresentationMapper.toListarRequest(UsuarioId.de(authentication.getName()), objetivoId))));
    }

    @GetMapping("/{miniObjetivoId}/candidatos")
    @Operation(summary = "Hasta 3 alternativas para cambiar un mini-objetivo, calculadas con los datos de hoy")
    public ResponseEntity<List<CandidatoMiniObjetivoResponse>> candidatos(
            Authentication authentication, @PathVariable ObjetivoId objetivoId,
            @PathVariable MiniObjetivoId miniObjetivoId) {
        return ResponseEntity.ok(miniObjetivoPresentationMapper.toResponse(listarCandidatos.execute(
                miniObjetivoPresentationMapper.toCandidatosRequest(
                        UsuarioId.de(authentication.getName()), objetivoId, miniObjetivoId))));
    }

    @PostMapping("/{miniObjetivoId}/reemplazo")
    @Operation(summary = "Cambiar un mini-objetivo en curso por una de sus alternativas")
    public ResponseEntity<MiniObjetivoResponse> reemplazar(
            Authentication authentication, @PathVariable ObjetivoId objetivoId,
            @PathVariable MiniObjetivoId miniObjetivoId, @Valid @RequestBody ReemplazarMiniObjetivoWebRequest request) {
        return ResponseEntity.ok(miniObjetivoPresentationMapper.toResponse(reemplazarMiniObjetivo.execute(
                miniObjetivoPresentationMapper.toReemplazarRequest(
                        UsuarioId.de(authentication.getName()), objetivoId, miniObjetivoId, request))));
    }

    @DeleteMapping("/{miniObjetivoId}")
    @Operation(summary = "Quitar un mini-objetivo en curso")
    public ResponseEntity<Void> abandonar(
            Authentication authentication, @PathVariable ObjetivoId objetivoId,
            @PathVariable MiniObjetivoId miniObjetivoId) {
        abandonarMiniObjetivo.execute(miniObjetivoPresentationMapper.toAbandonarRequest(
                UsuarioId.de(authentication.getName()), objetivoId, miniObjetivoId));
        return ResponseEntity.noContent().build();
    }
}
