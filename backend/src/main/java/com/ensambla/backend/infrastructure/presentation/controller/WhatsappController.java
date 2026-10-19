package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.whatsapp.GenerarCodigoWhatsappUseCase;
import com.ensambla.backend.domain.useCase.whatsapp.RecibirMensajesWhatsappUseCase;
import com.ensambla.backend.infrastructure.presentation.dto.whatsapp.CodigoWhatsappResponse;
import com.ensambla.backend.infrastructure.presentation.mapper.whatsapp.WhatsappPresentationMapper;
import com.ensambla.backend.infrastructure.whatsapp.LectorWebhookMeta;
import com.ensambla.backend.infrastructure.whatsapp.VerificadorWebhookMeta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/whatsapp")
@Tag(name = "WhatsApp")
@RequiredArgsConstructor
public class WhatsappController {

    private final GenerarCodigoWhatsappUseCase generarCodigoWhatsappUseCase;

    private final RecibirMensajesWhatsappUseCase recibirMensajesWhatsappUseCase;

    private final WhatsappPresentationMapper whatsappPresentationMapper;

    private final VerificadorWebhookMeta verificadorWebhookMeta;

    private final LectorWebhookMeta lectorWebhookMeta;

    @PostMapping("/vinculo/codigo")
    @Operation(summary = "Generar el código que se manda por WhatsApp para vincular el número con el negocio")
    public ResponseEntity<CodigoWhatsappResponse> generarCodigo(Authentication authentication) {
        CodigoWhatsappResponse response = whatsappPresentationMapper.toResponse(generarCodigoWhatsappUseCase.execute(
                whatsappPresentationMapper.toGenerarCodigoRequest(UUID.fromString(authentication.getName()))));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/webhook")
    @Operation(summary = "Confirmación del webhook que hace Meta al registrarlo (devuelve el challenge)")
    public ResponseEntity<String> verificar(@RequestParam(name = "hub.mode", required = false) String modo,
                                            @RequestParam(name = "hub.verify_token", required = false) String token,
                                            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (!verificadorWebhookMeta.suscripcionValida(modo, token)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(challenge);
    }

    /** Meta reintenta si no recibe 200, así que un aviso válido responde 200 aunque no traiga nada para guardar. */
    @PostMapping("/webhook")
    @Operation(summary = "Aviso de Meta con los mensajes que llegaron al número de Ensambla")
    public ResponseEntity<Void> recibir(@RequestHeader(name = "X-Hub-Signature-256", required = false) String firma,
                                        @RequestBody byte[] cuerpo) {
        if (!verificadorWebhookMeta.firmaValida(cuerpo, firma)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        recibirMensajesWhatsappUseCase.execute(lectorWebhookMeta.leer(cuerpo));
        return ResponseEntity.ok().build();
    }
}
