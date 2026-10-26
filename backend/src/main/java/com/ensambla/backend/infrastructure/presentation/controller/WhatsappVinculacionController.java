package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoVinculacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.VinculoWhatsappResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/whatsapp/vinculacion")
@Tag(name = "WhatsApp")
@RequiredArgsConstructor
public class WhatsappVinculacionController {

    private final GenerarCodigoVinculacion generarCodigoVinculacion;
    private final ConsultarVinculacionWhatsapp consultarVinculacionWhatsapp;
    private final DesvincularWhatsapp desvincularWhatsapp;
    private final WhatsappPresentationMapper mapper;

    @PostMapping("/codigo")
    @Operation(summary = "Generar un código de 10 minutos para vincular el número; se manda al bot como VINCULAR <código>")
    public ResponseEntity<CodigoVinculacionResponse> generarCodigo(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(generarCodigoVinculacion.execute(
                mapper.toGenerarCodigoRequest(usuarioDe(authentication)))));
    }

    @GetMapping
    @Operation(summary = "Número de WhatsApp vinculado al usuario; 404 si no tiene ninguno")
    public ResponseEntity<VinculoWhatsappResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(
                consultarVinculacionWhatsapp.execute(mapper.toConsultarRequest(usuarioDe(authentication)))));
    }

    @DeleteMapping
    @Operation(summary = "Desvincular el número de WhatsApp; deja de poder cargar datos")
    public ResponseEntity<Void> desvincular(Authentication authentication) {
        desvincularWhatsapp.execute(mapper.toDesvincularRequest(usuarioDe(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
