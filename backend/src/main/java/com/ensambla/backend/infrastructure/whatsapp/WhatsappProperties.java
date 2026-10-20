package com.ensambla.backend.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de la carga por WhatsApp ({@code ensambla.whatsapp.*}).
 *
 * @param proveedor      {@code simulado} (por defecto, no envía nada) o {@code evolution}
 * @param numeroBot      número al que el usuario le escribe, sólo dígitos con código de país
 * @param webhookSecreto valor que el proveedor manda en {@code X-Ensambla-Webhook-Token}; vacío rechaza todo
 * @param evolution      conexión con Evolution API
 */
@ConfigurationProperties(prefix = "ensambla.whatsapp")
public record WhatsappProperties(
        @DefaultValue("simulado") String proveedor,
        @DefaultValue("") String numeroBot,
        @DefaultValue("") String webhookSecreto,
        @DefaultValue Evolution evolution
) {

    /**
     * @param url       base de Evolution API, por ejemplo {@code http://localhost:8081}
     * @param apiKey    clave global de Evolution; sólo por variable de entorno o archivo de secretos
     * @param instancia nombre de la instancia (el número escaneado con el QR)
     */
    public record Evolution(
            @DefaultValue("http://localhost:8081") String url,
            @DefaultValue("") String apiKey,
            @DefaultValue("ensambla") String instancia
    ) {
    }
}
