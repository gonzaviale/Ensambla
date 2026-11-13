package com.ensambla.backend.infrastructure.catalogo;

import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IndiceSepaTest {

    private static final String VALIDO = """
            {"productos":{"00000077903754":{"codigo":"77903754","nombre":"Alfajor",
            "marca":"Marca","presentacion":"40 g"}}}
            """;

    @Test
    @DisplayName("Carga la lista incluida en el JAR sin rutas de la computadora de desarrollo")
    void buscarShouldLoadPackagedCatalog() {
        IndiceSepa indice = new IndiceSepa(new ClassPathResource("catalogos/sepa.json.gz"));

        var result = indice.buscar(new CodigoBarras("7790895000997"));

        assertThat(result).isPresent();
        assertThat(result.orElseThrow().nombre()).containsIgnoringCase("COCA COLA");
    }

    @Test
    @DisplayName("Admite JSON y gzip y resuelve GTIN equivalentes preservando los datos")
    void buscarShouldLoadBothFormats() throws Exception {
        for (boolean comprimido : new boolean[]{false, true}) {
            IndiceSepa indice = new IndiceSepa(givenRecurso(VALIDO, comprimido));

            var result = indice.buscar(new CodigoBarras("00000077903754")).orElseThrow();

            assertThat(result.codigo()).isEqualTo("77903754");
            assertThat(result.nombre()).isEqualTo("Alfajor");
            assertThat(result.marca()).isEqualTo("Marca");
            assertThat(result.presentacion()).isEqualTo("40 g");
            assertThat(result.imagen()).isNull();
            assertThat(indice.buscar(new CodigoBarras("77905741"))).isEmpty();
        }
    }

    @Test
    @DisplayName("Rechaza un índice corrupto o vacío sin publicar un catálogo parcial")
    void constructorShouldRejectCorruptIndex() throws Exception {
        for (String json : new String[]{"{", "{}", "{\"productos\":[]}", "{\"productos\":{}}",
                VALIDO.replace("00000077903754", "otra"), VALIDO.replace("Alfajor", " "),
                VALIDO.replace("77903754\"", "77903755\"")}) {
            var recurso = givenRecurso(json, false);

            assertThatThrownBy(() -> new IndiceSepa(recurso)).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    @DisplayName("Rechaza un índice que excede el límite de memoria")
    void constructorShouldRejectOversizedIndex() {
        var recurso = new ByteArrayResource(new byte[50_000_001]) {
            @Override
            public String getFilename() {
                return "catalogo.json";
            }
        };

        assertThatThrownBy(() -> new IndiceSepa(recurso)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Una ruta configurada inexistente impide arrancar con un catálogo silenciosamente incompleto")
    void constructorShouldRejectMissingResource() {
        var recurso = new ClassPathResource("catalogos/no-existe.json.gz");

        assertThatThrownBy(() -> new IndiceSepa(recurso)).isInstanceOf(IllegalStateException.class);
    }

    // --- arrange ---
    private ByteArrayResource givenRecurso(String json, boolean comprimido) throws Exception {
        byte[] contenido = json.getBytes(StandardCharsets.UTF_8);
        if (comprimido) {
            var salida = new ByteArrayOutputStream();
            try (var gzip = new GZIPOutputStream(salida)) {
                gzip.write(contenido);
            }
            contenido = salida.toByteArray();
        }
        return new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return comprimido ? "catalogo.json.gz" : "catalogo.json";
            }
        };
    }
}
