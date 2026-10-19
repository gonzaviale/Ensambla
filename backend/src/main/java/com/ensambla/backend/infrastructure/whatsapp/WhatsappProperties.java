package com.ensambla.backend.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración del webhook de WhatsApp ({@code ensambla.whatsapp.*}). Sólo por variable de entorno o archivo de
 * secretos; vacíos, el webhook rechaza todo.
 *
 * @param verifyToken texto que se carga en el panel de Meta para confirmar la URL del webhook
 * @param appSecret   secreto de la app de Meta con el que se comprueba la firma de cada aviso
 */
@ConfigurationProperties(prefix = "ensambla.whatsapp")
public record WhatsappProperties(
        @DefaultValue("") String verifyToken,
        @DefaultValue("") String appSecret
) {
}
