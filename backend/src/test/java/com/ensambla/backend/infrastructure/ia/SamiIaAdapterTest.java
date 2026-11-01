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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @BeforeEach
    void setUp() {
        consulta = new ConsultaIaSami("vendo cosas", List.of(), SamiPruebas.plantillas());
    }

    @Test
    @DisplayName("Si el modelo chico responde bien y con confianza, no se consulta al grande")
    void proponerShouldStopAtFirstModelWhenConfident() {
        PropuestaIaSami result = adapter(modelo("haiku", VENTAS_ALTA), modelo("sonnet", GASTOS_ALTA)).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
        assertThat(pedidos).hasSize(1);
    }

    @Test
    @DisplayName("Si el chico no está seguro, escala al grande y suma el consumo de los dos")
    void proponerShouldEscalateWhenFirstModelIsNotConfident() {
        PropuestaIaSami result = adapter(modelo("haiku", VENTAS_BAJA), modelo("sonnet", GASTOS_ALTA)).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Gastos");
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku + sonnet", 200, 40));
        assertThat(pedidos).hasSize(2);
    }

    @Test
    @DisplayName("Si el chico inventa una plantilla, escala al grande")
    void proponerShouldEscalateWhenFirstModelInventsATemplate() {
        PropuestaIaSami result = adapter(modelo("haiku", INVENTADA), modelo("sonnet", GASTOS_ALTA)).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Gastos");
    }

    @Test
    @DisplayName("Si el chico devuelve basura, escala al grande")
    void proponerShouldEscalateWhenFirstModelReturnsGarbage() {
        PropuestaIaSami result = adapter(modelo("haiku", "esto no es json"), modelo("sonnet", GASTOS_ALTA))
                .proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Gastos");
    }

    @Test
    @DisplayName("Si ninguno acierta, devuelve una propuesta sin plantilla pero con todo lo gastado")
    void proponerShouldReturnEmptyProposalWithConsumptionWhenNoneIsValid() {
        PropuestaIaSami result = adapter(modelo("haiku", INVENTADA), modelo("sonnet", "null")).proponer(consulta);

        assertThat(result.plantilla()).isNull();
        assertThat(result.columnasExtra()).isEmpty();
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku + sonnet", 200, 40));
    }

    @Test
    @DisplayName("Si el grande falla en responder bien, se conserva la propuesta del chico")
    void proponerShouldKeepFirstProposalWhenSecondIsInvalid() {
        PropuestaIaSami result = adapter(modelo("haiku", VENTAS_BAJA), modelo("sonnet", INVENTADA)).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.consumo().modelo()).isEqualTo("haiku + sonnet");
    }

    @Test
    @DisplayName("Si el grande se cae, se conserva la propuesta del chico y su consumo")
    void proponerShouldKeepFirstProposalWhenSecondModelFails() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalStateException("sin cuota");
        };

        PropuestaIaSami result = adapter(modelo("haiku", VENTAS_BAJA), roto).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
    }

    @Test
    @DisplayName("Si el primer modelo se cae, propaga el error")
    void proponerShouldPropagateErrorWhenFirstModelFails() {
        ClienteModeloTexto roto = peticion -> {
            throw new IllegalStateException("timeout");
        };

        assertThatThrownBy(() -> adapter(roto, modelo("sonnet", GASTOS_ALTA)).proponer(consulta))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("timeout");
    }

    @Test
    @DisplayName("Un JSON vacío o con valor nulo no es una propuesta")
    void proponerShouldTreatBlankOrNullJsonAsInvalid() {
        assertThat(adapter(modelo("haiku", null)).proponer(consulta).plantilla()).isNull();
        assertThat(adapter(modelo("haiku", "  ")).proponer(consulta).plantilla()).isNull();
        assertThat(adapter(modelo("haiku", "null")).proponer(consulta).plantilla()).isNull();
    }

    @Test
    @DisplayName("Tolera campos de más y toma la confianza como cero si falta")
    void proponerShouldTolerateUnknownFieldsAndMissingConfidence() {
        String json = "{\"plantilla\":\"Ventas\",\"extra\":true,\"columnas\":[]}";

        PropuestaIaSami result = adapter(modelo("haiku", json)).proponer(consulta);

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.confianza()).isZero();
    }

    @Test
    @DisplayName("Convierte las columnas y descarta las de un tipo desconocido o ausente")
    void proponerShouldParseColumnsAndSkipUnknownTypes() {
        String json = """
                {"plantilla":"Ventas","confianza":0.9,"columnas":[
                  {"nombre":"Saldo pendiente","tipo":"NUMERO","opciones":[]},
                  {"nombre":"Forma de pago","tipo":"OPCION","opciones":["Efectivo","Tarjeta"]},
                  {"nombre":"Raro","tipo":"INVENTADO","opciones":[]},
                  {"nombre":"Sin tipo","opciones":[]}]}
                """;

        PropuestaIaSami result = adapter(modelo("haiku", json)).proponer(consulta);

        assertThat(result.columnasExtra()).containsExactly(
                new ColumnaSugerida("Saldo pendiente", TipoCampo.NUMERO, List.of()),
                new ColumnaSugerida("Forma de pago", TipoCampo.OPCION, List.of("Efectivo", "Tarjeta")));
    }

    @Test
    @DisplayName("Una propuesta sin columnas en el JSON queda con la lista vacía")
    void proponerShouldHandleMissingColumns() {
        PropuestaIaSami result = adapter(modelo("haiku", "{\"plantilla\":\"Ventas\",\"confianza\":0.9}"))
                .proponer(consulta);

        assertThat(result.columnasExtra()).isEmpty();
    }

    @Test
    @DisplayName("Al modelo le llegan las plantillas disponibles, el texto y las aclaraciones")
    void proponerShouldSendTemplatesTextAndClarifications() {
        consulta = new ConsultaIaSami("vendo cosas", List.of("¿Qué anotás? → Lo que vendo"),
                SamiPruebas.plantillas());

        adapter(modelo("haiku", VENTAS_ALTA)).proponer(consulta);

        PeticionModeloTexto pedido = pedidos.getFirst();
        assertThat(pedido.contexto())
                .contains("- Ventas: columnas Producto (TEXTO), Monto (NUMERO)")
                .contains("- Inventario: columnas Nombre (TEXTO), Stock (NUMERO)")
                .contains("vendo cosas")
                .contains("- ¿Qué anotás? → Lo que vendo");
        assertThat(pedido.instrucciones()).contains("Sos Sami");
        assertThat(pedido.esquemaJson()).contains("\"plantilla\"");
    }

    private SamiIaAdapter adapter(ClienteModeloTexto... modelos) {
        return new SamiIaAdapter(List.of(modelos), prompt, JsonMapper.builder().build(), UMBRAL_ESCALAR);
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
