package com.ensambla.backend.domain.useCase.sami.conversarConSami;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.plantilla.PlantillaProyecto;
import com.ensambla.backend.domain.model.sami.ColumnaDiccionario;
import com.ensambla.backend.domain.model.sami.DiccionarioSami;
import com.ensambla.backend.domain.model.sami.PlantillaDiccionario;
import com.ensambla.backend.domain.model.sami.RespuestaSami;
import com.ensambla.backend.domain.model.sami.TerminoDiccionario;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** La primera etapa de Sami: lo que el diccionario deduce del texto, sin preguntar ni consultar a la IA. */
class ConversarConSamiInterpretacionTest {

    private static final double UMBRAL = 0.7;
    private static final String TEXTO_AMBIGUO = "quiero un stock y llevar los pedidos";

    private final DiccionarioSami diccionario = SamiPruebas.diccionarioReal();
    private final List<PlantillaProyecto> plantillas = SamiPruebas.plantillas();

    @Test
    @DisplayName("«Vendo velas y anoto quién me debe» sugiere Ventas con saldo pendiente y confianza sobre el umbral")
    void executeShouldSuggestVentasWithSaldoPendienteForCandleSeller() {
        PropuestaSamiItem result = interpretar("Vendo velas y anoto quién me debe").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.columnas()).extracting(ColumnaSamiItem::nombre).containsExactly("Saldo pendiente");
        assertThat(result.confianza()).isGreaterThanOrEqualTo(UMBRAL);
    }

    @Test
    @DisplayName("Entiende sinónimos, plurales y mayúsculas sin importar las tildes")
    void executeShouldMatchSynonymsPluralsAndAccents() {
        PropuestaSamiItem result = interpretar("LLEVO EL STOCK de mi MERCADERÍA, me quedo sin cosas").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.confianza()).isGreaterThanOrEqualTo(UMBRAL);
    }

    @Test
    @DisplayName("Sin coincidencias no sugiere plantilla")
    void executeShouldSuggestNothingWhenNothingMatches() {
        assertThat(interpretar("hola, quiero probar la aplicación")).isEmpty();
    }

    @Test
    @DisplayName("Un texto vacío se trata como sin coincidencias")
    void executeShouldHandleEmptyText() {
        assertThat(interpretar("")).isEmpty();
    }

    @Test
    @DisplayName("Si dos plantillas empatan, la confianza es cero para que Sami pregunte")
    void executeShouldHaveZeroConfidenceOnTie() {
        PropuestaSamiItem result = interpretar(TEXTO_AMBIGUO).orElseThrow();

        assertThat(result.confianza()).isZero();
    }

    @Test
    @DisplayName("Con poca evidencia la confianza queda baja aunque no haya otra candidata")
    void executeShouldHaveLowConfidenceWithLittleEvidence() {
        PropuestaSamiItem result = interpretar("vendo velas").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Ventas");
        assertThat(result.confianza()).isEqualTo(0.6);
    }

    @Test
    @DisplayName("Entre varias candidatas sugiere la de mayor puntaje")
    void executeShouldSuggestTheBestScoredCandidate() {
        PropuestaSamiItem result = interpretar("vendo y llevo el stock de mi inventario").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.confianza()).isLessThan(1.0);
    }

    @Test
    @DisplayName("Ignora las plantillas del diccionario que no están disponibles")
    void executeShouldIgnoreUnavailableTemplates() {
        List<PlantillaProyecto> sinVentas = plantillas.stream()
                .filter(plantilla -> !plantilla.nombre().equals("Ventas"))
                .toList();

        PropuestaSamiItem result = SamiPruebas.interpretar(
                diccionario, "vendo y llevo el stock de mi inventario", sinVentas).orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.confianza()).as("sin Ventas no queda otra candidata cerca").isEqualTo(1.0);
    }

    @Test
    @DisplayName("No propone una columna que la plantilla ya trae")
    void executeShouldNotSuggestColumnsTheTemplateAlreadyHas() {
        PropuestaSamiItem result = interpretar("tengo una agenda de mis clientes con su teléfono").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Clientes");
        assertThat(result.columnas()).isEmpty();
    }

    @Test
    @DisplayName("Sólo propone columnas pensadas para la plantilla sugerida")
    void executeShouldOnlySuggestColumnsForTheSuggestedTemplate() {
        PropuestaSamiItem result = interpretar("llevo el stock y el vencimiento, y cobro con tarjeta").orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.columnas()).extracting(ColumnaSamiItem::nombre).containsExactly("Vencimiento");
    }

    @Test
    @DisplayName("Propone la forma de pago con sus opciones")
    void executeShouldSuggestPaymentMethodWithOptions() {
        PropuestaSamiItem result = interpretar("vendo ropa y me pagan por transferencia o efectivo").orElseThrow();

        assertThat(result.columnas()).singleElement().satisfies(columna -> {
            assertThat(columna.nombre()).isEqualTo("Forma de pago");
            assertThat(columna.tipo()).isEqualTo(TipoCampo.OPCION.name());
            assertThat(columna.opciones()).containsExactly("Efectivo", "Transferencia", "Tarjeta");
        });
    }

    @Test
    @DisplayName("Una columna sin plantillas indicadas aplica a cualquiera")
    void executeShouldApplyColumnWithoutTemplateRestrictionToAny() {
        DiccionarioSami propio = new DiccionarioSami(
                List.of(new PlantillaDiccionario("Ventas", List.of(new TerminoDiccionario("vendo", 5)))),
                List.of(new ColumnaDiccionario("Notas", TipoCampo.TEXTO, null, List.of("anotaciones"), null)),
                null);

        PropuestaSamiItem result = SamiPruebas.interpretar(
                propio, "vendo cosas y hago anotaciones", plantillas).orElseThrow();

        assertThat(result.columnas()).extracting(ColumnaSamiItem::nombre).containsExactly("Notas");
        assertThat(result.confianza()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Una columna pensada para cualquier plantilla no se repite si la sugerida ya la trae")
    void executeShouldNotRepeatColumnThatAppliesToAnyTemplateWhenItIsAlreadyThere() {
        DiccionarioSami propio = new DiccionarioSami(
                List.of(new PlantillaDiccionario("Clientes", List.of(new TerminoDiccionario("agenda", 5)))),
                List.of(new ColumnaDiccionario("Teléfono", TipoCampo.TEXTO, null, List.of("telefono"), null)),
                null);

        PropuestaSamiItem result = SamiPruebas.interpretar(propio, "mi agenda con el teléfono", plantillas)
                .orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Clientes");
        assertThat(result.columnas()).isEmpty();
    }

    @Test
    @DisplayName("Elegir una opción decide la plantilla y descarta a las que esa pregunta separaba")
    void executeShouldDecideTemplateWhenOptionIsChosen() {
        PropuestaSamiItem result = interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "stock")).orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.confianza()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("La opción «ninguna de estas» descarta todas las plantillas de la pregunta")
    void executeShouldDiscardAllTemplatesWhenNoneOfThese() {
        assertThat(interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "otra"))).isEmpty();
    }

    @Test
    @DisplayName("Una respuesta posterior puede recuperar una plantilla que una anterior había descartado")
    void executeShouldLetLaterAnswerRecoverDiscardedTemplate() {
        PropuestaSamiItem result = interpretar("hola",
                new RespuestaSami("principal", "ventas"),
                new RespuestaSami("venta-o-stock", "stock")).orElseThrow();

        assertThat(result.plantilla()).isEqualTo("Inventario");
        assertThat(result.confianza()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Rechaza una respuesta a una pregunta que no existe")
    void executeShouldRejectUnknownQuestion() {
        assertThatThrownBy(() -> interpretar(TEXTO_AMBIGUO, new RespuestaSami("inventada", "x")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La pregunta inventada no existe");
    }

    @Test
    @DisplayName("Rechaza una opción que la pregunta no tiene")
    void executeShouldRejectUnknownOption() {
        assertThatThrownBy(() -> interpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "inventada")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("La opción inventada no existe en la pregunta principal");
    }

    // --- act ---
    private Optional<PropuestaSamiItem> interpretar(String texto, RespuestaSami... respuestas) {
        return SamiPruebas.interpretar(diccionario, texto, plantillas, respuestas);
    }
}
