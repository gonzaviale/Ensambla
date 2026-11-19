package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class IndiceSepaTest {

    private static final String VALIDO = """
            {"productos":{"00000077903754":{"codigo":"77903754","nombre":"Alfajor",
            "marca":"Marca","presentacion":"40 g"}}}
            """;

    private IndiceSepa indice;
    private Optional<ProductoCatalogo> encontrado;
    private Throwable rechazo;

    static Stream<String> indicesCorruptos() {
        return Stream.of("{", "{}", "{\"productos\":[]}", "{\"productos\":{}}",
                VALIDO.replace("00000077903754", "otra"), VALIDO.replace("Alfajor", " "),
                VALIDO.replace("77903754\"", "77903755\""));
    }

    @Test
    @DisplayName("Carga la lista incluida en el JAR sin rutas de la computadora de desarrollo")
    void cargaLaListaIncluidaEnElJarSinRutasDeLaComputadoraDeDesarrollo() {
        dadoElIndiceEmpaquetadoEnElJar();

        cuandoSeBuscaElCodigo("7790895000997");

        entoncesSeEncuentraUnaGaseosa();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("Admite JSON y gzip y resuelve GTIN equivalentes preservando los datos")
    void admiteJsonYGzipYResuelveGtinEquivalentesPreservandoLosDatos(boolean comprimido) throws Exception {
        dadoUnIndiceValido(comprimido);

        cuandoSeBuscaElCodigo("00000077903754");

        entoncesSeEncuentraElAlfajorConTodosSusDatos();
        entoncesUnCodigoAjenoNoSeEncuentra();
    }

    @ParameterizedTest
    @MethodSource("indicesCorruptos")
    @DisplayName("Rechaza un índice corrupto o vacío sin publicar un catálogo parcial")
    void rechazaUnIndiceCorruptoOVacioSinPublicarUnCatalogoParcial(String json) {
        cuandoSeIntentaCargarElIndice(recursoSinComprimir(json));

        entoncesSeRechazaElIndice();
    }

    @Test
    @DisplayName("Rechaza un índice que excede el límite de memoria")
    void rechazaUnIndiceQueExcedeElLimiteDeMemoria() {
        cuandoSeIntentaCargarElIndice(unRecursoDeMasDeCincuentaMegas());

        entoncesSeRechazaElIndice();
    }

    @Test
    @DisplayName("Una ruta configurada inexistente impide arrancar con un catálogo silenciosamente incompleto")
    void unaRutaConfiguradaInexistenteImpideArrancarConUnCatalogoSilenciosamenteIncompleto() {
        cuandoSeIntentaCargarElIndice(new ClassPathResource("catalogos/no-existe.json.gz"));

        entoncesSeRechazaElIndice();
    }

    // --- dado ---

    private void dadoElIndiceEmpaquetadoEnElJar() {
        indice = new IndiceSepa(new ClassPathResource("catalogos/sepa.json.gz"));
    }

    private void dadoUnIndiceValido(boolean comprimido) throws Exception {
        indice = new IndiceSepa(unRecurso(VALIDO, comprimido));
    }

    // --- cuando ---

    private void cuandoSeBuscaElCodigo(String codigo) {
        encontrado = indice.buscar(new CodigoBarras(codigo));
    }

    private void cuandoSeIntentaCargarElIndice(Resource recurso) {
        rechazo = catchThrowable(() -> new IndiceSepa(recurso));
    }

    // --- entonces ---

    private void entoncesSeEncuentraUnaGaseosa() {
        assertThat(encontrado).isPresent();
        assertThat(encontrado.orElseThrow().nombre()).containsIgnoringCase("COCA COLA");
    }

    private void entoncesSeEncuentraElAlfajorConTodosSusDatos() {
        ProductoCatalogo producto = encontrado.orElseThrow();
        assertThat(producto.codigo()).isEqualTo("77903754");
        assertThat(producto.nombre()).isEqualTo("Alfajor");
        assertThat(producto.marca()).isEqualTo("Marca");
        assertThat(producto.presentacion()).isEqualTo("40 g");
        assertThat(producto.imagen()).isNull();
    }

    private void entoncesUnCodigoAjenoNoSeEncuentra() {
        assertThat(indice.buscar(new CodigoBarras("77905741"))).isEmpty();
    }

    private void entoncesSeRechazaElIndice() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }

    // --- datos ---

    private Resource recursoSinComprimir(String json) {
        try {
            return unRecurso(json, false);
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private Resource unRecursoDeMasDeCincuentaMegas() {
        return new ByteArrayResource(new byte[50_000_001]) {
            @Override
            public String getFilename() {
                return "catalogo.json";
            }
        };
    }

    private ByteArrayResource unRecurso(String json, boolean comprimido) throws Exception {
        byte[] contenido = json.getBytes(StandardCharsets.UTF_8);
        byte[] bytes = comprimido ? comprimir(contenido) : contenido;
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return comprimido ? "catalogo.json.gz" : "catalogo.json";
            }
        };
    }

    private byte[] comprimir(byte[] contenido) throws Exception {
        var salida = new ByteArrayOutputStream();
        try (var gzip = new GZIPOutputStream(salida)) {
            gzip.write(contenido);
        }
        return salida.toByteArray();
    }
}
