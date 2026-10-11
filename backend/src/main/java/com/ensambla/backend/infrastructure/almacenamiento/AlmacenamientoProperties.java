package com.ensambla.backend.infrastructure.almacenamiento;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.time.Duration;

/**
 * Configuración del almacenamiento de archivos compatible con S3 ({@code ensambla.almacenamiento.*}). En desarrollo
 * apunta al MinIO del {@code docker-compose.yml}.
 *
 * @param endpoint          URL del servicio S3; las URLs firmadas se arman con este host
 * @param region            región de firma (MinIO acepta cualquiera)
 * @param accessKey         credencial; sólo por variable de entorno o archivo de secretos
 * @param secretKey         credencial; sólo por variable de entorno o archivo de secretos
 * @param bucket            bucket privado donde se guardan los archivos
 * @param expiracionUrl     cuánto vale una URL firmada
 * @param inicializarBucket si es {@code true}, el bucket se crea al arrancar cuando no existe
 */
@ConfigurationProperties(prefix = "ensambla.almacenamiento")
public record AlmacenamientoProperties(
        URI endpoint,
        @DefaultValue("us-east-1") String region,
        String accessKey,
        String secretKey,
        @DefaultValue("ensambla-archivos") String bucket,
        @DefaultValue("15m") Duration expiracionUrl,
        @DefaultValue("true") boolean inicializarBucket
) {
}
