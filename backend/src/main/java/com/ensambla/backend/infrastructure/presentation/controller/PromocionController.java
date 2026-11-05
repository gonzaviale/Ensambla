package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.promocion.redactarPromocion.RedactarPromocion;
import com.ensambla.backend.domain.useCase.promocion.sugerirPromociones.SugerirPromociones;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.RedactarPromocionWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.SugerenciasDePromocionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.promocion.TextoDePromocionResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.promocion.PromocionPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Promociones")
@RequiredArgsConstructor
public class PromocionController {

    private final SugerirPromociones sugerirPromociones;

    private final RedactarPromocion redactarPromocion;

    private final PromocionPresentationMapper presentationMapper;

    @GetMapping("/proyectos/{proyectoId}/promociones")
    @Operation(summary = "Qué conviene promocionar según lo que vende el sistema, y los canales donde compartirlo")
    public ResponseEntity<SugerenciasDePromocionResponse> sugerir(Authentication authentication,
                                                                  @PathVariable ProyectoId proyectoId) {
        return ResponseEntity.ok(presentationMapper.toResponse(sugerirPromociones.execute(
                presentationMapper.toSugerirRequest(usuarioDe(authentication), proyectoId))));
    }

    @PostMapping("/promociones/texto")
    @Operation(summary = "Escribir una promoción con el tono y el largo del canal elegido")
    public ResponseEntity<TextoDePromocionResponse> redactar(Authentication authentication,
                                                             @Valid @RequestBody RedactarPromocionWebRequest request) {
        return ResponseEntity.ok(presentationMapper.toResponse(redactarPromocion.execute(
                presentationMapper.toRedactarRequest(usuarioDe(authentication), request))));
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
