package com.ensambla.backend.infrastructure.ia;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * Configuración de la lectura con IA ({@code ensambla.ia.*}).
 *
 * @param proveedor            {@code simulado} (por defecto, sin costo) o el proveedor real que se conecte
 * @param modelo               id del modelo multimodal del proveedor
 * @param apiKey               credencial del proveedor; sólo por variable de entorno o archivo de secretos
 * @param timeout              tiempo máximo de espera de una lectura
 * @param maxPaginasPorLectura cuántas páginas se mandan juntas como máximo
 * @param umbralConfianza      debajo de este valor un dato se marca como dudoso
 */
@ConfigurationProperties(prefix = "ensambla.ia")
public record IaProperties(
        @DefaultValue("simulado") String proveedor,
        String modelo,
        String apiKey,
        @DefaultValue("60s") Duration timeout,
        @DefaultValue("10") int maxPaginasPorLectura,
        @DefaultValue("0.8") double umbralConfianza
) {
}
