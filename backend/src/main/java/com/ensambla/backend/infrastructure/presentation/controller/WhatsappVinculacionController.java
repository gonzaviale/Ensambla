package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.whatsapp.ConsultarVinculacionWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.DesvincularWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.GenerarCodigoVinculacionUseCase;
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

import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp/vinculacion")
@Tag(name = "WhatsApp")
@RequiredArgsConstructor
public class WhatsappVinculacionController {

    private final GenerarCodigoVinculacionUseCase generarCodigoVinculacionUseCase;
    private final ConsultarVinculacionWhatsappUseCase consultarVinculacionWhatsappUseCase;
    private final DesvincularWhatsappUseCase desvincularWhatsappUseCase;
    private final WhatsappPresentationMapper mapper;

    @PostMapping("/codigo")
    @Operation(summary = "Generar un código de 10 minutos para vincular el número; se manda al bot como VINCULAR <código>")
    public ResponseEntity<CodigoVinculacionResponse> generarCodigo(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(generarCodigoVinculacionUseCase.execute(usuarioDe(authentication))));
    }

    @GetMapping
    @Operation(summary = "Número de WhatsApp vinculado al usuario; 404 si no tiene ninguno")
    public ResponseEntity<VinculoWhatsappResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(
                consultarVinculacionWhatsappUseCase.execute(usuarioDe(authentication))));
    }

    @DeleteMapping
    @Operation(summary = "Desvincular el número de WhatsApp; deja de poder cargar datos")
    public ResponseEntity<Void> desvincular(Authentication authentication) {
        desvincularWhatsappUseCase.execute(usuarioDe(authentication));
        return ResponseEntity.noContent().build();
    }

    private static UUID usuarioDe(Authentication authentication) {
        return UUID.fromString(authentication.getName());
    }
}
