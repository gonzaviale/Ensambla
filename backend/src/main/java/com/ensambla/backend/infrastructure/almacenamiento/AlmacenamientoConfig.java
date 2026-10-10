package com.ensambla.backend.infrastructure.almacenamiento;

import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Cableado del almacenamiento de archivos con el SDK de S3. MinIO exige direcciones con el bucket en la ruta
 * ({@code path-style}), así que se fuerza en el cliente y en el firmador.
 */
@Configuration
@EnableConfigurationProperties(AlmacenamientoProperties.class)
public class AlmacenamientoConfig {

    @Bean(destroyMethod = "close")
    S3Client s3Client(AlmacenamientoProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(credenciales(properties))
                .forcePathStyle(true)
                .build();
    }

    @Bean(destroyMethod = "close")
    S3Presigner s3Presigner(AlmacenamientoProperties properties) {
        return S3Presigner.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region()))
                .credentialsProvider(credenciales(properties))
                .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                .build();
    }

    @Bean
    AlmacenamientoArchivosPort almacenamientoArchivos(S3Presigner presigner, AlmacenamientoProperties properties) {
        return new AlmacenamientoS3Adapter(presigner, properties.bucket(), properties.expiracionUrl());
    }

    @Bean
    @ConditionalOnProperty(prefix = "ensambla.almacenamiento", name = "inicializar-bucket", havingValue = "true",
            matchIfMissing = true)
    InicializadorBucket inicializadorBucket(S3Client s3Client, AlmacenamientoProperties properties) {
        return new InicializadorBucket(s3Client, properties.bucket());
    }

    private StaticCredentialsProvider credenciales(AlmacenamientoProperties properties) {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
    }
}
