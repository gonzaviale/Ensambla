package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.model.usuario.UsuarioId;
import com.ensambla.backend.domain.useCase.whatsapp.confirmarVerificacionWhatsapp.ConfirmarVerificacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.consultarVinculacionWhatsapp.ConsultarVinculacionWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.desvincularWhatsapp.DesvincularWhatsapp;
import com.ensambla.backend.domain.useCase.whatsapp.generarCodigoVinculacion.GenerarCodigoVinculacion;
import com.ensambla.backend.domain.useCase.whatsapp.solicitarVerificacionWhatsapp.SolicitarVerificacionWhatsapp;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoVinculacionResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.ConfirmarVerificacionWhatsappWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.SolicitarVerificacionWhatsappWebRequest;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.VerificacionWhatsappResponse;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.VinculoWhatsappResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp")
@RequiredArgsConstructor
public class WhatsappVinculacionController {

    private final SolicitarVerificacionWhatsapp solicitarVerificacionWhatsapp;
    private final ConfirmarVerificacionWhatsapp confirmarVerificacionWhatsapp;
    private final GenerarCodigoVinculacion generarCodigoVinculacion;
    private final ConsultarVinculacionWhatsapp consultarVinculacionWhatsapp;
    private final DesvincularWhatsapp desvincularWhatsapp;
    private final WhatsappPresentationMapper mapper;

    @PostMapping("/verificacion")
    @Operation(summary = "Cargar el número de WhatsApp: se le manda por WhatsApp un código de 6 dígitos para verificarlo")
    public ResponseEntity<VerificacionWhatsappResponse> solicitarVerificacion(Authentication authentication,
            @Valid @RequestBody SolicitarVerificacionWhatsappWebRequest request) {
        return ResponseEntity.ok(mapper.toResponse(solicitarVerificacionWhatsapp.execute(
                mapper.toSolicitarVerificacionRequest(usuarioDe(authentication), request))));
    }

    @PostMapping("/verificacion/confirmar")
    @Operation(summary = "Confirmar el código que llegó por WhatsApp: vincula el número y habilita a Sami por el chat")
    public ResponseEntity<VinculoWhatsappResponse> confirmarVerificacion(Authentication authentication,
            @Valid @RequestBody ConfirmarVerificacionWhatsappWebRequest request) {
        return ResponseEntity.ok(mapper.toResponse(confirmarVerificacionWhatsapp.execute(
                mapper.toConfirmarVerificacionRequest(usuarioDe(authentication), request))));
    }

    @PostMapping("/vinculacion/codigo")
    @Operation(summary = "Generar un código de 10 minutos para vincular el número; se manda al bot como VINCULAR <código>")
    public ResponseEntity<CodigoVinculacionResponse> generarCodigo(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(generarCodigoVinculacion.execute(
                mapper.toGenerarCodigoRequest(usuarioDe(authentication)))));
    }

    @GetMapping("/vinculacion")
    @Operation(summary = "Número de WhatsApp vinculado al usuario; 404 si no tiene ninguno")
    public ResponseEntity<VinculoWhatsappResponse> consultar(Authentication authentication) {
        return ResponseEntity.ok(mapper.toResponse(
                consultarVinculacionWhatsapp.execute(mapper.toConsultarRequest(usuarioDe(authentication)))));
    }

    @DeleteMapping("/vinculacion")
    @Operation(summary = "Desvincular el número de WhatsApp; deja de poder cargar datos")
    public ResponseEntity<Void> desvincular(Authentication authentication) {
        desvincularWhatsapp.execute(mapper.toDesvincularRequest(usuarioDe(authentication)));
        return ResponseEntity.noContent().build();
    }

    private static UsuarioId usuarioDe(Authentication authentication) {
        return UsuarioId.de(authentication.getName());
    }
}
