package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.aviso.AvisoId;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.aviso.aplicarAviso.AplicarAviso;
import com.ensambla.backend.domain.useCase.aviso.contarAvisos.ContarAvisos;
import com.ensambla.backend.domain.useCase.aviso.descartarAviso.DescartarAviso;
import com.ensambla.backend.domain.useCase.aviso.listarAvisos.ListarAvisos;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.AvisoResponse;
import com.ensambla.backend.infrastructure.presentation.dto.aviso.ContadorAvisosResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.aviso.AvisoPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/avisos")
@Tag(name = "Avisos")
@RequiredArgsConstructor
public class AvisoController {

    private final ListarAvisos listarAvisos;
    private final ContarAvisos contarAvisos;
    private final AplicarAviso aplicarAviso;
    private final DescartarAviso descartarAviso;
    private final AvisoPresentationMapper avisoPresentationMapper;

    @GetMapping
    @Operation(summary = "Listar los avisos del negocio, los pendientes primero; con estado, sólo los de ese estado")
    public ResponseEntity<List<AvisoResponse>> listar(Authentication authentication,
            @RequestParam(required = false) EstadoAviso estado) {
        return ResponseEntity.ok(avisoPresentationMapper.toResponse(listarAvisos.execute(
                avisoPresentationMapper.toListarRequest(usuarioDe(authentication), estado))));
    }

    @GetMapping("/contador")
    @Operation(summary = "Cantidad de avisos pendientes y cuántos son urgentes")
    public ResponseEntity<ContadorAvisosResponse> contar(Authentication authentication) {
        return ResponseEntity.ok(avisoPresentationMapper.toResponse(
                contarAvisos.execute(usuarioDe(authentication))));
    }

    @PostMapping("/{avisoId}/aplicar")
    @Operation(summary = "Ejecutar la acción sugerida del aviso y cerrarlo, en una sola operación")
    public ResponseEntity<AvisoResponse> aplicar(Authentication authentication, @PathVariable AvisoId avisoId) {
        return ResponseEntity.ok(avisoPresentationMapper.toResponse(aplicarAviso.execute(
                avisoPresentationMapper.toResolverRequest(usuarioDe(authentication), avisoId))));
    }

    @PostMapping("/{avisoId}/descartar")
    @Operation(summary = "Descartar el aviso; no vuelve a aparecer mientras no cambien los datos que lo originaron")
    public ResponseEntity<AvisoResponse> descartar(Authentication authentication, @PathVariable AvisoId avisoId) {
        return ResponseEntity.ok(avisoPresentationMapper.toResponse(descartarAviso.execute(
                avisoPresentationMapper.toResolverRequest(usuarioDe(authentication), avisoId))));
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
