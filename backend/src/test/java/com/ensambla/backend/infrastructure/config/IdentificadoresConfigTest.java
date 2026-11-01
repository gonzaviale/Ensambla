package com.ensambla.backend.infrastructure.config;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.common.Identificador;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.format.support.DefaultFormattingConversionService;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentificadoresConfigTest {

    private static final String TEXTO = "b0000000-0000-4000-8000-000000000001";

    private final IdentificadoresConfig config = new IdentificadoresConfig();
    private final JsonMapper json = JsonMapper.builder().addModule(config.identificadoresModule()).build();

    /** Un id sin {@code de(String)}: no lo puede leer ni el JSON ni una ruta. */
    record SinFabrica(UUID valor) implements Identificador {
    }

    /** Un id cuyo {@code de(String)} falla por algo que no es el texto. */
    record FabricaRota(UUID valor) implements Identificador {

        public static FabricaRota de(String valor) {
            throw new IllegalStateException("rota");
        }
    }

    record Fila(BloqueId bloque, Map<String, CampoId> campos) {
    }

    @Test
    @DisplayName("En el JSON un id viaja como el texto de su UUID, ida y vuelta")
    void jsonShouldWriteAndReadIdsAsUuidText() {
        Fila fila = new Fila(BloqueId.de(TEXTO), Map.of("monto", CampoId.de(TEXTO)));

        String escrito = json.writeValueAsString(fila);

        assertThat(escrito).isEqualTo("{\"bloque\":\"" + TEXTO + "\",\"campos\":{\"monto\":\"" + TEXTO + "\"}}");
        assertThat(json.readValue(escrito, Fila.class)).isEqualTo(fila);
    }

    @Test
    @DisplayName("Un texto que no es UUID no se lee como id")
    void jsonShouldRejectInvalidId() {
        assertThatThrownBy(() -> json.readValue("{\"bloque\":\"no-es-uuid\"}", Fila.class))
                .isInstanceOf(DatabindException.class);
    }

    @Test
    @DisplayName("Las rutas y parámetros se convierten al id de su tipo")
    void converterShouldParsePathVariables() {
        DefaultFormattingConversionService conversiones = new DefaultFormattingConversionService();
        config.addFormatters(conversiones);

        assertThat(conversiones.convert(" " + TEXTO + " ", BloqueId.class)).isEqualTo(BloqueId.de(TEXTO));
    }

    @Test
    @DisplayName("Un texto que no es UUID se rechaza como argumento inválido")
    void crearShouldRejectInvalidText() {
        assertThatThrownBy(() -> IdentificadoresConfig.crear(BloqueId.class, "no-es-uuid"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Un id sin de(String) es un error de programación")
    void crearShouldFailForTypeWithoutFactory() {
        assertThatThrownBy(() -> IdentificadoresConfig.crear(SinFabrica.class, TEXTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("SinFabrica no tiene de(String)");
    }

    @Test
    @DisplayName("Si de(String) falla por otra razón no se confunde con un texto inválido")
    void crearShouldWrapOtherFactoryFailures() {
        assertThatThrownBy(() -> IdentificadoresConfig.crear(FabricaRota.class, TEXTO))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No se pudo leer el id FabricaRota")
                .cause().hasMessage("rota");
    }

    @Test
    @DisplayName("Los tipos que no son ids siguen con su lectura de siempre")
    void jsonShouldKeepDefaultDeserializersForOtherTypes() {
        assertThat(json.readValue("{\"monto\":3}", Map.class)).containsEntry("monto", 3);
    }
}
