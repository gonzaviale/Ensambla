package com.ensambla.backend.infrastructure.almacenamiento;

import com.ensambla.backend.domain.model.archivo.UrlFirmada;
import com.ensambla.backend.domain.port.AlmacenamientoArchivosPort;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Prueba contra un MinIO real que los archivos del bucket sólo se pueden subir y leer con una URL firmada vigente.
 * Cada test usa un bucket nuevo, creado por {@link InicializadorBucket}.
 */
class AlmacenamientoS3IntegrationTest {

    // Misma imagen que el docker-compose.yml. Contenedor compartido por los tests de la clase; Ryuk lo elimina al final.
    private static final MinIOContainer MINIO = new MinIOContainer(
            DockerImageName.parse("cgr.dev/chainguard/minio:latest").asCompatibleSubstituteFor("minio/minio"));

    private static final String CLAVE = "negocios/1/fotos/pagina-1.jpg";
    private static final String TIPO_CONTENIDO = "image/jpeg";
    private static final byte[] FOTO = {1, 2, 3, 4};
    private static final Duration EXPIRACION = Duration.ofMinutes(15);

    static {
        MINIO.start();
    }

    private final HttpClient http = HttpClient.newHttpClient();
    private final AlmacenamientoConfig config = new AlmacenamientoConfig();

    private AlmacenamientoProperties properties;
    private S3Client s3;
    private S3Presigner presigner;
    private AlmacenamientoArchivosPort almacenamiento;

    @BeforeEach
    void setUp() {
        properties = givenProperties(EXPIRACION);
        s3 = config.s3Client(properties);
        presigner = config.s3Presigner(properties);
        config.inicializadorBucket(s3, properties).run(null);
        almacenamiento = config.almacenamientoArchivos(presigner, properties);
    }

    @AfterEach
    void tearDown() {
        s3.close();
        presigner.close();
    }

    @Test
    @DisplayName("Con la URL firmada de subida se sube el archivo y con la de lectura se lee el mismo contenido")
    void generarUrlsShouldUploadAndReadWhenSigned() throws Exception {
        givenFotoSubida();

        HttpResponse<byte[]> respuesta = get(almacenamiento.generarUrlLectura(CLAVE).url());

        assertThat(respuesta.statusCode()).isEqualTo(200);
        assertThat(respuesta.body()).isEqualTo(FOTO);
    }

    @Test
    @DisplayName("Sin firma el archivo no se puede leer")
    void leerShouldBeForbiddenWhenUrlIsNotSigned() throws Exception {
        givenFotoSubida();

        HttpResponse<byte[]> respuesta = get(properties.endpoint() + "/" + properties.bucket() + "/" + CLAVE);

        assertThat(respuesta.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("Con la firma alterada el archivo no se puede leer")
    void leerShouldBeForbiddenWhenSignatureIsTampered() throws Exception {
        givenFotoSubida();
        String url = almacenamiento.generarUrlLectura(CLAVE).url();

        HttpResponse<byte[]> respuesta = get(url.replaceFirst("X-Amz-Signature=[0-9a-f]+", "X-Amz-Signature=" + "0".repeat(64)));

        assertThat(respuesta.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("La URL de subida no sirve para leer el archivo")
    void leerShouldBeForbiddenWhenUsingUploadUrl() throws Exception {
        UrlFirmada subida = givenFotoSubida();

        HttpResponse<byte[]> respuesta = get(subida.url());

        // MinIO responde 400 o 403 según qué parte de la firma no coincide; en ningún caso entrega el archivo.
        assertThat(respuesta.statusCode()).isBetween(400, 499);
        assertThat(respuesta.body()).isNotEqualTo(FOTO);
    }

    @Test
    @DisplayName("Una URL firmada vencida ya no permite leer el archivo")
    void leerShouldBeForbiddenWhenUrlExpired() throws Exception {
        givenFotoSubida();
        AlmacenamientoArchivosPort conExpiracionCorta =
                config.almacenamientoArchivos(presigner, givenProperties(Duration.ofSeconds(1)));
        String url = conExpiracionCorta.generarUrlLectura(CLAVE).url();
        Thread.sleep(2_000);

        HttpResponse<byte[]> respuesta = get(url);

        assertThat(respuesta.statusCode()).isEqualTo(403);
    }

    @Test
    @DisplayName("La URL firmada informa cuándo vence según la expiración configurada")
    void generarUrlLecturaShouldExpireAfterConfiguredDuration() {
        Instant antes = Instant.now();

        UrlFirmada url = almacenamiento.generarUrlLectura(CLAVE);

        assertThat(url.expiraEn()).isBetween(antes.plus(EXPIRACION).minusSeconds(1), Instant.now().plus(EXPIRACION));
    }

    @Test
    @DisplayName("Si el bucket ya existe, el inicializador no falla")
    void inicializadorShouldDoNothingWhenBucketExists() {
        InicializadorBucket inicializador = config.inicializadorBucket(s3, properties);

        assertThatCode(() -> inicializador.run(null)).doesNotThrowAnyException();
        assertThatCode(() -> s3.headBucket(peticion -> peticion.bucket(properties.bucket())))
                .doesNotThrowAnyException();
    }

    // --- arrange ---
    private AlmacenamientoProperties givenProperties(Duration expiracion) {
        String bucket = properties == null ? "ensambla-" + UUID.randomUUID() : properties.bucket();
        return new AlmacenamientoProperties(URI.create(MINIO.getS3URL()), "us-east-1", MINIO.getUserName(),
                MINIO.getPassword(), bucket, expiracion, true);
    }

    private UrlFirmada givenFotoSubida() throws IOException, InterruptedException {
        UrlFirmada subida = almacenamiento.generarUrlSubida(CLAVE, TIPO_CONTENIDO);
        HttpResponse<Void> respuesta = http.send(HttpRequest.newBuilder(URI.create(subida.url()))
                        .header("Content-Type", TIPO_CONTENIDO)
                        .PUT(HttpRequest.BodyPublishers.ofByteArray(FOTO))
                        .build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(200);
        return subida;
    }

    // --- act ---
    private HttpResponse<byte[]> get(String url) throws IOException, InterruptedException {
        return http.send(HttpRequest.newBuilder(URI.create(url)).GET().build(), HttpResponse.BodyHandlers.ofByteArray());
    }
}
