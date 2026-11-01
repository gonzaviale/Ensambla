package com.ensambla.backend.infrastructure.whatsapp;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de la carga por WhatsApp ({@code ensambla.whatsapp.*}).
 *
 * @param proveedor      {@code simulado} (por defecto, no envía nada) o {@code evolution}
 * @param numeroBot      número al que el usuario le escribe, sólo dígitos con código de país
 * @param webhookSecreto valor que el proveedor manda en {@code X-Ensambla-Webhook-Token} o en {@code ?token=}; vacío
 *                       rechaza todo
 * @param evolution      conexión con Evolution API
 * @param atencion       cómo atiende Sami los mensajes recibidos
 */
@ConfigurationProperties(prefix = "ensambla.whatsapp")
public record WhatsappProperties(
        @DefaultValue("simulado") String proveedor,
        @DefaultValue("") String numeroBot,
        @DefaultValue("") String webhookSecreto,
        @DefaultValue Evolution evolution,
        @DefaultValue Atencion atencion
) {

    /**
     * @param url       base de Evolution API, por ejemplo {@code http://localhost:8081}
     * @param apiKey    clave global de Evolution; sólo por variable de entorno o archivo de secretos
     * @param instancia nombre de la instancia (el número escaneado con el QR)
     * @param timeout   cuánto se espera una respuesta de Evolution (también al bajar una foto)
     */
    public record Evolution(
            @DefaultValue("http://localhost:8081") String url,
            @DefaultValue("") String apiKey,
            @DefaultValue("ensambla") String instancia,
            @DefaultValue("30s") Duration timeout
    ) {
    }

    /**
     * @param habilitada      si este proceso atiende la cola de mensajes (se apaga en los tests)
     * @param intervalo       cada cuánto mira si hay mensajes nuevos
     * @param vigenciaReclamo cuánto tiene un mensaje tomado antes de darse por perdido
     * @param umbralConfianza debajo de este valor un dato leído de una foto se marca como dudoso y un texto se
     *                        confirma eligiendo la tabla
     */
    public record Atencion(
            @DefaultValue("true") boolean habilitada,
            @DefaultValue("1500ms") Duration intervalo,
            @DefaultValue("2m") Duration vigenciaReclamo,
            @DefaultValue("0.6") double umbralConfianza
    ) {
    }
}
