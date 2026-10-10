package com.ensambla.backend.support;

import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * MinIO compartido por los tests durante la JVM; Ryuk lo elimina al finalizar. Usa la misma imagen que el
 * {@code docker-compose.yml} (MinIO ya no publica imágenes oficiales).
 */
public final class MinioTestContainer {

    public static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("cgr.dev/chainguard/minio:latest").asCompatibleSubstituteFor("minio/minio"));

    static {
        MINIO.start();
    }

    private MinioTestContainer() {
    }
}
