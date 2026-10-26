package com.ensambla.backend.infrastructure.presentation.controller;

import com.ensambla.backend.domain.useCase.whatsapp.procesarMensajeEntrante.ProcesarMensajeEntrante;
import com.ensambla.backend.infrastructure.whatsapp.EvolutionWebhookParser;
import com.ensambla.backend.infrastructure.whatsapp.WhatsappProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.JsonNode;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/whatsapp/webhook")
@Tag(name = "WhatsApp")
@RequiredArgsConstructor
public class WhatsappWebhookController {

    public static final String HEADER_TOKEN = "X-Ensambla-Webhook-Token";

    private final EvolutionWebhookParser parser;
    private final ProcesarMensajeEntrante procesarMensajeEntrante;
    private final WhatsappProperties properties;

    @PostMapping
    @Operation(summary = "Recibe los mensajes de WhatsApp que entrega el proveedor; se autentica con el token compartido")
    public ResponseEntity<Void> recibir(@RequestHeader(value = HEADER_TOKEN, required = false) String token,
            @RequestBody JsonNode cuerpo) {
        if (!tokenValido(token)) {
            return ResponseEntity.status(401).build();
        }
        parser.parsear(cuerpo).ifPresent(procesarMensajeEntrante::execute);
        return ResponseEntity.ok().build();
    }

    /** Sin secreto configurado se rechaza todo; la comparación no revela en qué posición difiere el token. */
    private boolean tokenValido(String token) {
        String secreto = properties.webhookSecreto();
        if (secreto.isBlank() || token == null) {
            return false;
        }
        return MessageDigest.isEqual(secreto.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8));
    }
}
