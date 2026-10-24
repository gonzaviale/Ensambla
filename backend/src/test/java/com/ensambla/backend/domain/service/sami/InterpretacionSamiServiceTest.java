package com.ensambla.backend.domain.service.sami;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.BloquePlantilla;
import com.ensambla.backend.domain.model.plantilla.EstructuraPlantilla.CampoPlantilla;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ColumnaDiccionario;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.InterpretacionSami;
import com.ensambla.backend.domain.model.sami.PlantillaDiccionario;
import com.ensambla.backend.domain.model.sami.TerminoDiccionario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class InterpretacionSamiServiceTest {

    private static final double UMBRAL = 0.7;

    private List<PlantillaProyecto> plantillas;
    private InterpretacionSamiService service;

    @BeforeEach
    void setUp() throws IOException {
        plantillas = List.of(
                plantilla("Inventario", campo("Nombre", TipoCampo.TEXTO), campo("Stock", TipoCampo.NUMERO)),
                plantilla("Pedidos", campo("Cliente", TipoCampo.TEXTO), campo("Entrega", TipoCampo.FECHA)),
                plantilla("Clientes", campo("Nombre", TipoCampo.TEXTO), campo("Teléfono", TipoCampo.TEXTO)),
                plantilla("Ventas", campo("Producto", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)),
                plantilla("Gastos", campo("Detalle", TipoCampo.TEXTO), campo("Monto", TipoCampo.NUMERO)));
        service = new InterpretacionSamiService(diccionarioReal());
    }

    @Test
    @DisplayName("«Vendo velas y anoto quién me debe» sugiere Ventas con saldo pendiente y confianza sobre el umbral")
    void interpretarShouldSuggestVentasWithSaldoPendienteForCandleSeller() {
        InterpretacionSami result = service.interpretar("Vendo velas y anoto quién me debe", plantillas, List.of());

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.columnasExtra()).extracting(ColumnaSugerida::nombre).containsExactly("Saldo pendiente");
        assertThat(result.alcanza(UMBRAL)).isTrue();
    }

    @Test
    @DisplayName("Entiende sinónimos, plurales y mayúsculas sin importar las tildes")
    void interpretarShouldMatchSynonymsPluralsAndAccents() {
        String texto = "LLEVO EL STOCK de mi MERCADERÍA, me quedo sin cosas";

        InterpretacionSami result = service.interpretar(texto, plantillas, List.of());

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.alcanza(UMBRAL)).isTrue();
    }

    @Test
    @DisplayName("Sin coincidencias no sugiere plantilla ni columnas y la confianza es cero")
    void interpretarShouldSuggestNothingWhenNothingMatches() {
        InterpretacionSami result = service.interpretar("hola, quiero probar la aplicación", plantillas, List.of());

        assertThat(result.plantilla()).isNull();
        assertThat(result.columnasExtra()).isEmpty();
        assertThat(result.puntajes()).isEmpty();
        assertThat(result.confianza()).isZero();
        assertThat(result.alcanza(UMBRAL)).isFalse();
    }

    @Test
    @DisplayName("Un texto vacío se trata como sin coincidencias")
    void interpretarShouldHandleEmptyText() {
        InterpretacionSami result = service.interpretar("", plantillas, List.of());

        assertThat(result.plantilla()).isNull();
    }

    @Test
    @DisplayName("Si dos plantillas empatan, la confianza es cero para que Sami pregunte")
    void interpretarShouldHaveZeroConfidenceOnTie() {
        InterpretacionSami result = service.interpretar("quiero un stock y llevar los pedidos", plantillas, List.of());

        assertThat(result.puntajes()).hasSize(2);
        assertThat(result.confianza()).isZero();
        assertThat(result.alcanza(UMBRAL)).isFalse();
    }

    @Test
    @DisplayName("Con poca evidencia la confianza queda baja aunque no haya otra candidata")
    void interpretarShouldHaveLowConfidenceWithLittleEvidence() {
        InterpretacionSami result = service.interpretar("vendo velas", plantillas, List.of());

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.confianza()).isEqualTo(0.6);
        assertThat(result.alcanza(UMBRAL)).isFalse();
    }

    @Test
    @DisplayName("Ordena las candidatas de mayor a menor puntaje")
    void interpretarShouldSortCandidatesByScore() {
        InterpretacionSami result = service.interpretar("vendo y llevo el stock de mi inventario", plantillas, List.of());

        assertThat(result.puntajes()).extracting("plantilla").containsExactly("Inventario", "Ventas");
        assertThat(result.plantilla()).isEqualTo("Inventario");
    }

    @Test
    @DisplayName("Ignora las plantillas del diccionario que no están disponibles")
    void interpretarShouldIgnoreUnavailableTemplates() {
        List<PlantillaProyecto> sinVentas = plantillas.stream()
                .filter(plantilla -> !plantilla.nombre().equals("Ventas"))
                .toList();

        InterpretacionSami result = service.interpretar("vendo y llevo el stock de mi inventario", sinVentas, List.of());

        assertThat(result.puntajes()).extracting("plantilla").containsExactly("Inventario");
    }

    @Test
    @DisplayName("No propone una columna que la plantilla ya trae")
    void interpretarShouldNotSuggestColumnsTheTemplateAlreadyHas() {
        String texto = "tengo una agenda de mis clientes con su teléfono";

        InterpretacionSami result = service.interpretar(texto, plantillas, List.of());

        assertThat(result.plantilla()).isEqualTo("Clientes");
        assertThat(result.columnasExtra()).isEmpty();
    }

    @Test
    @DisplayName("Sólo propone columnas pensadas para la plantilla sugerida")
    void interpretarShouldOnlySuggestColumnsForTheSuggestedTemplate() {
        String texto = "llevo el stock y el vencimiento, y cobro con tarjeta";

        InterpretacionSami result = service.interpretar(texto, plantillas, List.of());

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.columnasExtra()).extracting(ColumnaSugerida::nombre).containsExactly("Vencimiento");
    }

    @Test
    @DisplayName("Propone la forma de pago con sus opciones")
    void interpretarShouldSuggestPaymentMethodWithOptions() {
        String texto = "vendo ropa y me pagan por transferencia o efectivo";

        InterpretacionSami result = service.interpretar(texto, plantillas, List.of());

        assertThat(result.columnasExtra()).containsExactly(new ColumnaSugerida(
                "Forma de pago", TipoCampo.OPCION, List.of("Efectivo", "Transferencia", "Tarjeta")));
    }

    @Test
    @DisplayName("Una columna sin plantillas indicadas aplica a cualquiera")
    void interpretarShouldApplyColumnWithoutTemplateRestrictionToAny() {
        DiccionarioSami diccionario = new DiccionarioSami(
                List.of(new PlantillaDiccionario("Ventas", List.of(new TerminoDiccionario("vendo", 5)))),
                List.of(new ColumnaDiccionario("Notas", TipoCampo.TEXTO, null, List.of("anotaciones"), null)),
                null);

        InterpretacionSami result = new InterpretacionSamiService(diccionario)
                .interpretar("vendo cosas y hago anotaciones", plantillas, List.of());

        assertThat(result.columnasExtra()).extracting(ColumnaSugerida::nombre).containsExactly("Notas");
        assertThat(result.confianza()).isEqualTo(1.0);
    }

    private DiccionarioSami diccionarioReal() throws IOException {
        try (InputStream json = getClass().getResourceAsStream("/ia/sami-diccionario.json")) {
            return JsonMapper.builder().build().readValue(json, DiccionarioSami.class);
        }
    }

    private PlantillaProyecto plantilla(String nombre, CampoPlantilla... campos) {
        return new PlantillaProyecto(UUID.randomUUID(), nombre, "",
                new EstructuraPlantilla(List.of(new BloquePlantilla(nombre, List.of(campos)))));
    }

    private CampoPlantilla campo(String nombre, TipoCampo tipo) {
        return new CampoPlantilla(nombre, tipo, false, null, null);
    }
}
