package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.sami.ColumnaSugerida;
import com.ensambla.backend.domain.model.sami.ConsultaIaSami;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.PropuestaIaSami;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class SamiIaAdapterTest {

    private static final double UMBRAL_ESCALAR = 0.6;
    private static final String VENTAS_ALTA = json("Ventas", 0.9);
    private static final String VENTAS_BAJA = json("Ventas", 0.4);
    private static final String GASTOS_ALTA = json("Gastos", 0.95);
    private static final String INVENTADA = json("Peluquería", 0.9);

    private final PromptSami prompt = new PromptSami(
            new ClassPathResource("ia/prompt-sami.md"), new ClassPathResource("ia/esquema-sami.json"));
    private final List<PeticionModeloTexto> pedidos = new ArrayList<>();

    private ConsultaIaSami consulta;
    private List<ClienteModeloTexto> modelos = List.of();
    private PropuestaIaSami propuesta;
    private Throwable rechazo;

    @BeforeEach
    void setUp() {
        consulta = new ConsultaIaSami("vendo cosas", List.of(), SamiPruebas.plantillas());
    }

    @Test
    @DisplayName("Si el modelo chico responde bien y con confianza, no se consulta al grande")
    void siElModeloChicoRespondeBienYConConfianzaNoSeConsultaAlGrande() {
        dadoUnModeloChico(VENTAS_ALTA);
        dadoUnModeloGrande(GASTOS_ALTA);

        cuandoSePropone();

        entoncesSeProponeVentasConElConsumoDelChicoYSeConsultaUnaVez();
    }

    @Test
    @DisplayName("Si el chico no está seguro, escala al grande y suma el consumo de los dos")
    void siElChicoNoEstaSeguroEscalaAlGrandeYSumaElConsumoDeLosDos() {
        dadoUnModeloChico(VENTAS_BAJA);
        dadoUnModeloGrande(GASTOS_ALTA);

        cuandoSePropone();

        entoncesSeProponeGastosConElConsumoDeLosDos();
    }

    @Test
    @DisplayName("Si el chico inventa una plantilla, escala al grande")
    void siElChicoInventaUnaPlantillaEscalaAlGrande() {
        dadoUnModeloChico(INVENTADA);
        dadoUnModeloGrande(GASTOS_ALTA);

        cuandoSePropone();

        entoncesLaPlantillaEs("Gastos");
    }

    @Test
    @DisplayName("Si el chico devuelve basura, escala al grande")
    void siElChicoDevuelveBasuraEscalaAlGrande() {
        dadoUnModeloChico("esto no es json");
        dadoUnModeloGrande(GASTOS_ALTA);

        cuandoSePropone();

        entoncesLaPlantillaEs("Gastos");
    }

    @Test
    @DisplayName("Si ninguno acierta, devuelve una propuesta sin plantilla pero con todo lo gastado")
    void siNingunoAciertaDevuelveUnaPropuestaSinPlantillaPeroConTodoLoGastado() {
        dadoUnModeloChico(INVENTADA);
        dadoUnModeloGrande("null");

        cuandoSePropone();

        entoncesNoHayPlantillaNiColumnasPeroSeSumaElConsumoDeLosDos();
    }

    @Test
    @DisplayName("Si el grande falla en responder bien, se conserva la propuesta del chico")
    void siElGrandeFallaEnResponderBienSeConservaLaPropuestaDelChico() {
        dadoUnModeloChico(VENTAS_BAJA);
        dadoUnModeloGrande(INVENTADA);

        cuandoSePropone();

        entoncesSeConservaVentasConElConsumoDeLosDos();
    }

    @Test
    @DisplayName("Si el grande se cae, se conserva la propuesta del chico y su consumo")
    void siElGrandeSeCaeSeConservaLaPropuestaDelChicoYSuConsumo() {
        dadoUnModeloChico(VENTAS_BAJA);
        dadoUnModeloQueSeCaePor("sin cuota");

        cuandoSePropone();

        entoncesSeConservaVentasConElConsumoSoloDelChico();
    }

    @Test
    @DisplayName("Si el primer modelo se cae, propaga el error")
    void siElPrimerModeloSeCaePropagaElError() {
        dadoUnModeloQueSeCaePor("timeout");
        dadoUnModeloGrande(GASTOS_ALTA);

        cuandoSeIntentaProponer();

        entoncesSePropagaElErrorDeTimeout();
    }

    @Test
    @DisplayName("Un JSON nulo no es una propuesta")
    void unJsonNuloNoEsUnaPropuesta() {
        dadoUnModeloChico(null);

        cuandoSePropone();

        entoncesNoHayPlantilla();
    }

    @Test
    @DisplayName("Un JSON en blanco no es una propuesta")
    void unJsonEnBlancoNoEsUnaPropuesta() {
        dadoUnModeloChico("  ");

        cuandoSePropone();

        entoncesNoHayPlantilla();
    }

    @Test
    @DisplayName("Un JSON con valor nulo no es una propuesta")
    void unJsonConValorNuloNoEsUnaPropuesta() {
        dadoUnModeloChico("null");

        cuandoSePropone();

        entoncesNoHayPlantilla();
    }

    @Test
    @DisplayName("Tolera campos de más y toma la confianza como cero si falta")
    void toleraCamposDeMasYTomaLaConfianzaComoCeroSiFalta() {
        dadoUnModeloChico("{\"plantilla\":\"Ventas\",\"extra\":true,\"columnas\":[]}");

        cuandoSePropone();

        entoncesLaPlantillaEsVentasConConfianzaCero();
    }

    @Test
    @DisplayName("Convierte las columnas y descarta las de un tipo desconocido o ausente")
    void convierteLasColumnasYDescartaLasDeUnTipoDesconocidoOAusente() {
        dadoUnModeloChicoQueSugiereColumnasConTiposValidosEInvalidos();

        cuandoSePropone();

        entoncesSoloQuedanLasColumnasDeTipoConocido();
    }

    @Test
    @DisplayName("Una propuesta sin columnas en el JSON queda con la lista vacía")
    void unaPropuestaSinColumnasEnElJsonQuedaConLaListaVacia() {
        dadoUnModeloChico("{\"plantilla\":\"Ventas\",\"confianza\":0.9}");

        cuandoSePropone();

        entoncesNoHayColumnasExtra();
    }

    @Test
    @DisplayName("Al modelo le llegan las plantillas disponibles, el texto y las aclaraciones")
    void alModeloLeLlegaLasPlantillasDisponiblesElTextoYLasAclaraciones() {
        dadoUnaConsultaConUnaAclaracion();
        dadoUnModeloChico(VENTAS_ALTA);

        cuandoSePropone();

        entoncesElModeloRecibePlantillasTextoYAclaraciones();
    }

    // --- dado ---

    private void dadoUnModeloChico(String json) {
        modelos = List.of(modelo("haiku", json));
    }

    private void dadoUnModeloGrande(String json) {
        List<ClienteModeloTexto> todos = new ArrayList<>(modelos);
        todos.add(modelo("sonnet", json));
        modelos = todos;
    }

    private void dadoUnModeloQueSeCaePor(String motivo) {
        List<ClienteModeloTexto> todos = new ArrayList<>(modelos);
        todos.add(peticion -> {
            throw new IllegalStateException(motivo);
        });
        modelos = todos;
    }

    private void dadoUnModeloChicoQueSugiereColumnasConTiposValidosEInvalidos() {
        dadoUnModeloChico("""
                {"plantilla":"Ventas","confianza":0.9,"columnas":[
                  {"nombre":"Saldo pendiente","tipo":"NUMERO","opciones":[]},
                  {"nombre":"Forma de pago","tipo":"OPCION","opciones":["Efectivo","Tarjeta"]},
                  {"nombre":"Raro","tipo":"INVENTADO","opciones":[]},
                  {"nombre":"Sin tipo","opciones":[]}]}
                """);
    }

    private void dadoUnaConsultaConUnaAclaracion() {
        consulta = new ConsultaIaSami("vendo cosas", List.of("¿Qué anotás? → Lo que vendo"), SamiPruebas.plantillas());
    }

    // --- cuando ---

    private void cuandoSePropone() {
        propuesta = adapter().proponer(consulta);
    }

    private void cuandoSeIntentaProponer() {
        rechazo = catchThrowable(this::cuandoSePropone);
    }

    // --- entonces ---

    private void entoncesSeProponeVentasConElConsumoDelChicoYSeConsultaUnaVez() {
        assertThat(propuesta.plantilla()).isEqualTo("Ventas");
        assertThat(propuesta.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
        assertThat(pedidos).hasSize(1);
    }

    private void entoncesSeProponeGastosConElConsumoDeLosDos() {
        assertThat(propuesta.plantilla()).isEqualTo("Gastos");
        assertThat(propuesta.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku + sonnet", 200, 40));
        assertThat(pedidos).hasSize(2);
    }

    private void entoncesLaPlantillaEs(String esperada) {
        assertThat(propuesta.plantilla()).isEqualTo(esperada);
    }

    private void entoncesNoHayPlantillaNiColumnasPeroSeSumaElConsumoDeLosDos() {
        assertThat(propuesta.plantilla()).isNull();
        assertThat(propuesta.columnasExtra()).isEmpty();
        assertThat(propuesta.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku + sonnet", 200, 40));
    }

    private void entoncesSeConservaVentasConElConsumoDeLosDos() {
        assertThat(propuesta.plantilla()).isEqualTo("Ventas");
        assertThat(propuesta.consumo().modelo()).isEqualTo("haiku + sonnet");
    }

    private void entoncesSeConservaVentasConElConsumoSoloDelChico() {
        assertThat(propuesta.plantilla()).isEqualTo("Ventas");
        assertThat(propuesta.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
    }

    private void entoncesSePropagaElErrorDeTimeout() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class).hasMessage("timeout");
    }

    private void entoncesNoHayPlantilla() {
        assertThat(propuesta.plantilla()).isNull();
    }

    private void entoncesLaPlantillaEsVentasConConfianzaCero() {
        assertThat(propuesta.plantilla()).isEqualTo("Ventas");
        assertThat(propuesta.confianza()).isZero();
    }

    private void entoncesSoloQuedanLasColumnasDeTipoConocido() {
        assertThat(propuesta.columnasExtra()).containsExactly(
                new ColumnaSugerida("Saldo pendiente", TipoCampo.NUMERO, List.of()),
                new ColumnaSugerida("Forma de pago", TipoCampo.OPCION, List.of("Efectivo", "Tarjeta")));
    }

    private void entoncesNoHayColumnasExtra() {
        assertThat(propuesta.columnasExtra()).isEmpty();
    }

    private void entoncesElModeloRecibePlantillasTextoYAclaraciones() {
        PeticionModeloTexto pedido = pedidos.getFirst();
        assertThat(pedido.contexto())
                .contains("- Ventas: columnas Producto (TEXTO), Monto (NUMERO)")
                .contains("- Inventario: columnas Nombre (TEXTO), Stock (NUMERO)")
                .contains("vendo cosas")
                .contains("- ¿Qué anotás? → Lo que vendo");
        assertThat(pedido.instrucciones()).contains("Sos Sami");
        assertThat(pedido.esquemaJson()).contains("\"plantilla\"");
    }

    // --- datos ---

    private SamiIaAdapter adapter() {
        return new SamiIaAdapter(modelos, prompt, JsonMapper.builder().build(), UMBRAL_ESCALAR);
    }

    private ClienteModeloTexto modelo(String nombre, String json) {
        return peticion -> {
            pedidos.add(peticion);
            return new RespuestaModeloTexto(json, "bedrock", nombre, 100, 20);
        };
    }

    private static String json(String plantilla, double confianza) {
        return "{\"plantilla\":\"%s\",\"confianza\":%s,\"columnas\":[]}".formatted(plantilla, confianza);
    }
}
