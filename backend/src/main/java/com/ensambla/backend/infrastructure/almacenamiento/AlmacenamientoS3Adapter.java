package com.ensambla.backend.infrastructure.almacenamiento;

import com.ensambla.backend.domain.model.archivo.UrlFirmada;
import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import software.amazon.awssdk.awscore.presigner.PresignedRequest;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;

/** Guarda archivos y genera URLs firmadas (SigV4) contra un bucket privado de S3 o MinIO. */
public class AlmacenamientoS3Adapter implements AlmacenamientoArchivosPort {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration expiracion;

    public AlmacenamientoS3Adapter(S3Client s3, S3Presigner presigner, String bucket, Duration expiracion) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = bucket;
        this.expiracion = expiracion;
    }

    @Override
    public void guardar(String clave, byte[] contenido, String tipoContenido) {
        s3.putObject(objeto -> objeto.bucket(bucket).key(clave).contentType(tipoContenido),
                RequestBody.fromBytes(contenido));
    }

    @Override
    public void eliminar(String clave) {
        s3.deleteObject(objeto -> objeto.bucket(bucket).key(clave));
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
