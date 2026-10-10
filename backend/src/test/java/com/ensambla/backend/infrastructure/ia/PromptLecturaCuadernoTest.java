package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.BloqueConocido;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PromptLecturaCuadernoTest {

    private static final UUID VENTAS_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final PaginaCuaderno PAGINA = new PaginaCuaderno(1, new byte[]{1}, "image/png");

    private PromptLecturaCuaderno prompt;

    @BeforeEach
    void setUp() {
        prompt = new PromptLecturaCuaderno(texto("Leé el cuaderno"), texto("{}"));
    }

    @Test
    @DisplayName("Arma la petición con instrucciones, esquema y páginas")
    void peticionShouldCarryInstruccionesEsquemaAndPaginas() {
        PeticionModeloVision peticion = prompt.peticion(List.of(PAGINA), List.of());

        assertThat(peticion.instrucciones()).isEqualTo("Leé el cuaderno");
        assertThat(peticion.esquemaJson()).isEqualTo("{}");
        assertThat(peticion.paginas()).containsExactly(PAGINA);
    }

    @Test
    @DisplayName("Sin bloques existentes le pide a la IA que proponga uno nuevo")
    void peticionShouldAskForNewBloqueWhenNegocioHasNoBloques() {
        PeticionModeloVision peticion = prompt.peticion(List.of(PAGINA), List.of());

        assertThat(peticion.contexto()).isEqualTo(PromptLecturaCuaderno.SIN_BLOQUES);
    }

    @Test
    @DisplayName("Lista los bloques existentes con su id, nombre y campos")
    void peticionShouldListBloquesExistentes() {
        BloqueConocido ventas = new BloqueConocido(VENTAS_ID, "Ventas", List.of("Fecha", "Monto"));

        PeticionModeloVision peticion = prompt.peticion(List.of(PAGINA), List.of(ventas));

        assertThat(peticion.contexto()).isEqualTo(PromptLecturaCuaderno.ENCABEZADO_BLOQUES
                + "\n- 00000000-0000-0000-0000-000000000001: Ventas [Fecha, Monto]");
    }

    @Test
    @DisplayName("Los recursos reales del classpath existen y piden la salida estructurada")
    void classpathResourcesShouldExist() {
        PromptLecturaCuaderno real = new PromptLecturaCuaderno(new ClassPathResource("ia/prompt-lectura-cuaderno.md"),
                new ClassPathResource("ia/esquema-lectura-cuaderno.json"));

        PeticionModeloVision peticion = real.peticion(List.of(PAGINA), List.of());

        assertThat(peticion.instrucciones()).contains("Respondé únicamente con el JSON");
        assertThat(peticion.esquemaJson()).contains("\"tipoRegistro\"", "\"confianza\"");
    }

    @Test
    @DisplayName("Falla al arrancar si falta un recurso de IA")
    void constructorShouldFailWhenResourceIsMissing() {
        ClassPathResource inexistente = new ClassPathResource("ia/no-existe.md");
        ByteArrayResource esquema = texto("{}");

        assertThatThrownBy(() -> new PromptLecturaCuaderno(inexistente, esquema))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No se pudo leer el recurso de IA");
    }

    // --- arrange ---
    private static ByteArrayResource texto(String contenido) {
        return new ByteArrayResource(contenido.getBytes(StandardCharsets.UTF_8));
    }
}
