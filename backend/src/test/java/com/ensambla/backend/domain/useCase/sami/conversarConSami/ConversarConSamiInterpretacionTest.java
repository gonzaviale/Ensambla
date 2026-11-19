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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/** La primera etapa de Sami: lo que el diccionario deduce del texto, sin preguntar ni consultar a la IA. */
class ConversarConSamiInterpretacionTest {

    private static final double UMBRAL = 0.7;
    private static final String TEXTO_AMBIGUO = "quiero un stock y llevar los pedidos";

    private DiccionarioSami diccionario = SamiPruebas.diccionarioReal();
    private List<PlantillaProyecto> plantillas = SamiPruebas.plantillas();
    private Optional<PropuestaSamiItem> propuesta;
    private Throwable error;

    @Test
    @DisplayName("«Vendo velas y anoto quién me debe» sugiere Ventas con saldo pendiente y confianza sobre el umbral")
    void vendoVelasYAnotoQuienMeDebeSugiereVentasConSaldoPendiente() {
        cuandoSeInterpreta("Vendo velas y anoto quién me debe");

        entoncesSugiere("Ventas");
        entoncesLasColumnasSon("Saldo pendiente");
        entoncesLaConfianzaSuperaElUmbral();
    }

    @Test
    @DisplayName("Entiende sinónimos, plurales y mayúsculas sin importar las tildes")
    void entiendeSinonimosPluralesYMayusculas() {
        cuandoSeInterpreta("LLEVO EL STOCK de mi MERCADERÍA, me quedo sin cosas");

        entoncesSugiere("Inventario");
        entoncesLaConfianzaSuperaElUmbral();
    }

    @ParameterizedTest
    @ValueSource(strings = {"hola, quiero probar la aplicación", ""})
    @DisplayName("Sin coincidencias, o con un texto vacío, no sugiere plantilla")
    void sinCoincidenciasNoSugierePlantilla(String texto) {
        cuandoSeInterpreta(texto);

        entoncesNoSugiereNada();
    }

    @Test
    @DisplayName("Si dos plantillas empatan, la confianza es cero para que Sami pregunte")
    void siDosPlantillasEmpatanLaConfianzaEsCero() {
        cuandoSeInterpreta(TEXTO_AMBIGUO);

        entoncesLaConfianzaEs(0.0);
    }

    @Test
    @DisplayName("Con poca evidencia la confianza queda baja aunque no haya otra candidata")
    void conPocaEvidenciaLaConfianzaQuedaBaja() {
        cuandoSeInterpreta("vendo velas");

        entoncesSugiere("Ventas");
        entoncesLaConfianzaEs(0.6);
    }

    @Test
    @DisplayName("Entre varias candidatas sugiere la de mayor puntaje")
    void entreVariasCandidatasSugiereLaDeMayorPuntaje() {
        cuandoSeInterpreta("vendo y llevo el stock de mi inventario");

        entoncesSugiere("Inventario");
        entoncesLaConfianzaNoEsTotal();
    }

    @Test
    @DisplayName("Ignora las plantillas del diccionario que no están disponibles")
    void ignoraLasPlantillasQueNoEstanDisponibles() {
        dadoQueNoEstaDisponibleLaPlantilla("Ventas");

        cuandoSeInterpreta("vendo y llevo el stock de mi inventario");

        entoncesSugiere("Inventario");
        entoncesLaConfianzaEs(1.0);
    }

    @Test
    @DisplayName("No propone una columna que la plantilla ya trae")
    void noProponeUnaColumnaQueLaPlantillaYaTrae() {
        cuandoSeInterpreta("tengo una agenda de mis clientes con su teléfono");

        entoncesSugiere("Clientes");
        entoncesNoHayColumnasExtra();
    }

    @Test
    @DisplayName("Sólo propone columnas pensadas para la plantilla sugerida")
    void soloProponeColumnasPensadasParaLaPlantillaSugerida() {
        cuandoSeInterpreta("llevo el stock y el vencimiento, y cobro con tarjeta");

        entoncesSugiere("Inventario");
        entoncesLasColumnasSon("Vencimiento");
    }

    @Test
    @DisplayName("Propone la forma de pago con sus opciones")
    void proponeLaFormaDePagoConSusOpciones() {
        cuandoSeInterpreta("vendo ropa y me pagan por transferencia o efectivo");

        entoncesLaUnicaColumnaEsDeOpciones("Forma de pago", "Efectivo", "Transferencia", "Tarjeta");
    }

    @Test
    @DisplayName("Una columna sin plantillas indicadas aplica a cualquiera")
    void unaColumnaSinPlantillasIndicadasAplicaACualquiera() {
        dadoUnDiccionarioDe("Ventas", "vendo", "Notas", "anotaciones");

        cuandoSeInterpreta("vendo cosas y hago anotaciones");

        entoncesLasColumnasSon("Notas");
        entoncesLaConfianzaEs(1.0);
    }

    @Test
    @DisplayName("Una columna pensada para cualquier plantilla no se repite si la sugerida ya la trae")
    void unaColumnaGeneralNoSeRepiteSiLaPlantillaYaLaTrae() {
        dadoUnDiccionarioDe("Clientes", "agenda", "Teléfono", "telefono");

        cuandoSeInterpreta("mi agenda con el teléfono");

        entoncesSugiere("Clientes");
        entoncesNoHayColumnasExtra();
    }

    @Test
    @DisplayName("Elegir una opción decide la plantilla y descarta a las que esa pregunta separaba")
    void elegirUnaOpcionDecideLaPlantilla() {
        cuandoSeInterpreta(TEXTO_AMBIGUO, new RespuestaSami("principal", "stock"));

        entoncesSugiere("Inventario");
        entoncesLaConfianzaEs(1.0);
    }

    @Test
    @DisplayName("La opción «ninguna de estas» descarta todas las plantillas de la pregunta")
    void laOpcionNingunaDeEstasDescartaTodasLasPlantillas() {
        cuandoSeInterpreta(TEXTO_AMBIGUO, new RespuestaSami("principal", "otra"));

        entoncesNoSugiereNada();
    }

    @Test
    @DisplayName("Una respuesta posterior puede recuperar una plantilla que una anterior había descartado")
    void unaRespuestaPosteriorRecuperaUnaPlantillaDescartada() {
        cuandoSeInterpreta("hola", new RespuestaSami("principal", "ventas"), new RespuestaSami("venta-o-stock", "stock"));

        entoncesSugiere("Inventario");
        entoncesLaConfianzaEs(1.0);
    }

    @Test
    @DisplayName("Rechaza una respuesta a una pregunta que no existe")
    void rechazaUnaRespuestaAUnaPreguntaQueNoExiste() {
        cuandoSeIntentaInterpretar(TEXTO_AMBIGUO, new RespuestaSami("inventada", "x"));

        entoncesSeRechazaPorque("La pregunta inventada no existe");
    }

    @Test
    @DisplayName("Rechaza una opción que la pregunta no tiene")
    void rechazaUnaOpcionQueLaPreguntaNoTiene() {
        cuandoSeIntentaInterpretar(TEXTO_AMBIGUO, new RespuestaSami("principal", "inventada"));

        entoncesSeRechazaPorque("La opción inventada no existe en la pregunta principal");
    }

    private void dadoQueNoEstaDisponibleLaPlantilla(String nombre) {
        plantillas = plantillas.stream().filter(plantilla -> !plantilla.nombre().equals(nombre)).toList();
    }

    /** Un diccionario con una sola plantilla reconocida por un término y una columna que aplica a cualquiera. */
    private void dadoUnDiccionarioDe(String plantilla, String termino, String columna, String disparador) {
        diccionario = new DiccionarioSami(
                List.of(new PlantillaDiccionario(plantilla, List.of(new TerminoDiccionario(termino, 5)))),
                List.of(new ColumnaDiccionario(columna, TipoCampo.TEXTO, null, List.of(disparador), null)),
                null);
    }

    private void cuandoSeInterpreta(String texto, RespuestaSami... respuestas) {
        propuesta = SamiPruebas.interpretar(diccionario, texto, plantillas, respuestas);
    }

    private void cuandoSeIntentaInterpretar(String texto, RespuestaSami... respuestas) {
        error = catchThrowable(() -> cuandoSeInterpreta(texto, respuestas));
    }

    private void entoncesSugiere(String plantilla) {
        assertThat(propuesta.orElseThrow().plantilla()).isEqualTo(plantilla);
    }

    private void entoncesNoSugiereNada() {
        assertThat(propuesta).isEmpty();
    }

    private void entoncesLasColumnasSon(String... nombres) {
        assertThat(propuesta.orElseThrow().columnas()).extracting(ColumnaSamiItem::nombre).containsExactly(nombres);
    }

    private void entoncesNoHayColumnasExtra() {
        assertThat(propuesta.orElseThrow().columnas()).isEmpty();
    }

    private void entoncesLaUnicaColumnaEsDeOpciones(String nombre, String... opciones) {
        ColumnaSamiItem columna = propuesta.orElseThrow().columnas().getFirst();
        assertThat(propuesta.orElseThrow().columnas()).hasSize(1);
        assertThat(columna.nombre()).isEqualTo(nombre);
        assertThat(columna.tipo()).isEqualTo(TipoCampo.OPCION.name());
        assertThat(columna.opciones()).containsExactly(opciones);
    }

    private void entoncesLaConfianzaSuperaElUmbral() {
        assertThat(propuesta.orElseThrow().confianza()).isGreaterThanOrEqualTo(UMBRAL);
    }

    private void entoncesLaConfianzaEs(double confianza) {
        assertThat(propuesta.orElseThrow().confianza()).isEqualTo(confianza);
    }

    private void entoncesLaConfianzaNoEsTotal() {
        assertThat(propuesta.orElseThrow().confianza()).isLessThan(1.0);
    }

    private void entoncesSeRechazaPorque(String mensaje) {
        assertThat(error).isInstanceOf(BusinessRuleException.class).hasMessage(mensaje);
    }
}