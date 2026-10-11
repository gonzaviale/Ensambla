package com.ensambla.backend.infrastructure.almacenamiento;

import com.ensambla.backend.domain.model.archivo.UrlFirmada;
import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

/** Genera URLs firmadas (SigV4) contra un bucket privado de S3 o MinIO. */
public class AlmacenamientoS3Adapter implements AlmacenamientoArchivosPort {

    private final S3Presigner presigner;
    private final String bucket;
    private final Duration expiracion;

    public AlmacenamientoS3Adapter(S3Presigner presigner, String bucket, Duration expiracion) {
        this.presigner = presigner;
        this.bucket = bucket;
        this.expiracion = expiracion;
    }

    @Override
    public UrlFirmada generarUrlSubida(String clave, String tipoContenido) {
        return aUrlFirmada(presigner.presignPutObject(firma -> firma
                .signatureDuration(expiracion)
                .putObjectRequest(objeto -> objeto.bucket(bucket).key(clave).contentType(tipoContenido))));
    }

    @Override
    public UrlFirmada generarUrlLectura(String clave) {
        return aUrlFirmada(presigner.presignGetObject(firma -> firma
                .signatureDuration(expiracion)
                .getObjectRequest(objeto -> objeto.bucket(bucket).key(clave))));
    }

    private UrlFirmada aUrlFirmada(PresignedRequest firmada) {
        return new UrlFirmada(firmada.url().toString(), firmada.expiration());
    }
}
