package com.ensambla.backend.infrastructure.almacenamiento;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;

/**
 * Crea el bucket al arrancar si todavía no existe. Se crea sin política de acceso, es decir privado: un archivo sólo se
 * lee o se escribe con una URL firmada.
 */
public class InicializadorBucket implements ApplicationRunner {

    private final S3Client s3;
    private final String bucket;

    public InicializadorBucket(S3Client s3, String bucket) {
        this.s3 = s3;
        this.bucket = bucket;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!existeBucket()) {
            s3.createBucket(peticion -> peticion.bucket(bucket));
        }
    }

    private boolean existeBucket() {
        try {
            s3.headBucket(peticion -> peticion.bucket(bucket));
            return true;
        } catch (NoSuchBucketException e) {
            return false;
        }
    }
}
