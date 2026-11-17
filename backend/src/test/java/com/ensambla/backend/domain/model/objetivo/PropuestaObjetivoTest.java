package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.CONSUMO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.ENTENDIDO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.OBJETIVO;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.RESPUESTA_IA;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.catalogo;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.manual;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.medido;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.respuestaIa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PropuestaObjetivoTest {

    private static final String METRICA_C1 = "c1";
    private static final String METRICA_C2 = "c2";
    private static final String METRICA_C3 = "c3";
    private static final String METRICA_C4 = "c4";
    private static final String METRICA_FUERA_DEL_CATALOGO = "c9";
    private static final int LARGO_MAXIMO_DE_RESPUESTA = 600;
    private static final int LARGO_MAXIMO_DE_ENTENDIDO = 300;

    private PropuestaObjetivoIa respuestaDeLaIa;
    private PropuestaObjetivo propuesta;
    private Throwable error;

    @Test
    @DisplayName("Mezcla una métrica del catálogo con una acción: el objetivo sería híbrido")
    void deShouldMixAMeasuredAndAManualMini() {
        givenUnaMetricaYUnaAccionValidas();

        whenSeArmaLaPropuesta();

        thenLaPropuestaTomaLosDatosDeLaIa();
        thenLosModosSonMedidoYManual();
        thenLaMetricaEsLaDelCatalogoYLaAccionLaDeLaIa();
        thenElTipoEs(TipoObjetivo.HIBRIDO);
        thenSePuedeGuardar();
    }

    @Test
    @DisplayName("Una métrica medida usa el texto y los números del servidor, nunca los de la IA")
    void deShouldUseTheServerNumbersForAMeasuredMini() {
        givenUnaRespuestaConMetricas(METRICA_C2);

        whenSeArmaLaPropuesta();

        thenLaUnicaMetricaEsLaDelCatalogoSinTextoDeIa();
        thenElTipoEs(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Una métrica fuera del catálogo se descarta")
    void deShouldDiscardAMetricOutsideTheCatalog() {
        givenUnaRespuestaConMetricas(METRICA_FUERA_DEL_CATALOGO, METRICA_C1);

        whenSeArmaLaPropuesta();

        thenLasMetricasSonLasDelCatalogo(0);
    }

    @Test
    @DisplayName("Dos métricas que miden lo mismo (mismo producto) se quedan en la primera")
    void deShouldDiscardTheMetricsThatClash() {
        givenUnaRespuestaConMetricas(METRICA_C1, METRICA_C4, METRICA_C2);

        whenSeArmaLaPropuesta();

        thenLasMetricasSonLasDelCatalogo(0, 1);
    }

    @Test
    @DisplayName("La misma métrica dos veces se queda con una sola")
    void deShouldDiscardARepeatedMetric() {
        givenUnaRespuestaConMetricas(METRICA_C2, METRICA_C2);

        whenSeArmaLaPropuesta();

        thenHayMinis(1);
    }

    @Test
    @DisplayName("Una acción con un importe o un porcentaje, o sin texto, se descarta")
    void deShouldDiscardAnInvalidAction() {
        givenUnaRespuestaConAccionesInvalidasYUnaValida();

        whenSeArmaLaPropuesta();

        thenSoloQuedaLaAccionValida();
    }

    @Test
    @DisplayName("Se quedan los primeros 3 mini-objetivos")
    void deShouldKeepAtMostThree() {
        givenTresMetricasYUnaAccion();

        whenSeArmaLaPropuesta();

        thenQuedanElMaximoDeMinisTodosMedidos();
    }

    @Test
    @DisplayName("Una meta en pesos no arma mini-objetivos aunque la IA los mande, y no se puede guardar")
    void deShouldNotProposeMinisForAMonetaryGoal() {
        givenUnaMetaEnPesosConMetricas();

        whenSeArmaLaPropuesta();

        thenLaPropuestaEsMonetariaSinMinisNiTipo();
        thenNoSePuedeGuardar();
    }

    @Test
    @DisplayName("Si no queda nada válido para guardar no hay tipo ni se puede guardar")
    void deShouldNotAllowSavingWithoutMinis() {
        givenUnaRespuestaConMetricas(METRICA_FUERA_DEL_CATALOGO);

        whenSeArmaLaPropuesta();

        thenNoHayMinisNiTipo();
        thenNoSePuedeGuardar();
    }

    @Test
    @DisplayName("Sin respuesta para la persona, la IA no sirve")
    void deShouldRejectAMissingAnswer() {
        givenUnaRespuestaDeLaIaConTexto(null);

        whenSeArmaLaPropuestaConError();

        thenLaIaNoSirve();
    }

    @Test
    @DisplayName("Con una respuesta en blanco para la persona, la IA no sirve")
    void deShouldRejectABlankAnswer() {
        givenUnaRespuestaDeLaIaConTexto("  ");

        whenSeArmaLaPropuestaConError();

        thenLaIaNoSirve();
    }

    @Test
    @DisplayName("Con una respuesta de más de 600 caracteres, la IA no sirve")
    void deShouldRejectATooLongAnswer() {
        givenUnaRespuestaDeLaIaConTexto("a".repeat(LARGO_MAXIMO_DE_RESPUESTA + 1));

        whenSeArmaLaPropuestaConError();

        thenLaIaNoSirveConElMensajeDeRespuestaInvalida();
    }

    @Test
    @DisplayName("Una respuesta de 600 caracteres se acepta y queda sin espacios en los extremos")
    void deShouldAcceptAnAnswerOfTheMaximumLength() {
        givenUnaRespuestaDeLaIaConTexto(" " + "a".repeat(LARGO_MAXIMO_DE_RESPUESTA) + " ");

        whenSeArmaLaPropuesta();

        thenLaRespuestaTieneElLargoMaximo();
    }

    @Test
    @DisplayName("Un «entendido» nulo queda sin entender")
    void deShouldDropANullUnderstanding() {
        givenUnaRespuestaDeLaIaConEntendido(null);

        whenSeArmaLaPropuesta();

        thenElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» vacío queda sin entender")
    void deShouldDropAnEmptyUnderstanding() {
        givenUnaRespuestaDeLaIaConEntendido("");

        whenSeArmaLaPropuesta();

        thenElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» de más de 300 caracteres queda sin entender")
    void deShouldDropATooLongUnderstanding() {
        givenUnaRespuestaDeLaIaConEntendido("a".repeat(LARGO_MAXIMO_DE_ENTENDIDO + 1));

        whenSeArmaLaPropuesta();

        thenElEntendidoEsNulo();
    }

    @Test
    @DisplayName("Un «entendido» válido queda sin espacios en los extremos")
    void deShouldKeepAValidUnderstandingStripped() {
        givenUnaRespuestaDeLaIaConEntendido("  " + ENTENDIDO + " ");

        whenSeArmaLaPropuesta();

        thenElEntendidoEs(ENTENDIDO);
    }

    @Test
    @DisplayName("El respaldo es un solo mini-objetivo manual de seguimiento con el objetivo de la persona")
    void respaldoShouldBeAWeeklyFollowUp() {
        whenSeArmaElRespaldo();

        thenElRespaldoEsUnSeguimientoSemanal();
    }

    @Test
    @DisplayName("Una acción elegida antes no impide elegir después una métrica del catálogo")
    void deShouldKeepAMeasuredMetricAfterAManualAction() {
        givenUnaAccionYDosMetricas();

        whenSeArmaLaPropuesta();

        thenLosModosSon(ModoMiniObjetivo.MANUAL, ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MEDIDO);
    }

    // --- arrange ---
    private void givenUnaMetricaYUnaAccionValidas() {
        respuestaDeLaIa = respuestaIa(medido(METRICA_C3), manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void givenUnaRespuestaConMetricas(String... metricas) {
        respuestaDeLaIa = respuestaIa(Arrays.stream(metricas).map(id -> medido(id)).toArray(EleccionIa[]::new));
    }

    private void givenUnaRespuestaConAccionesInvalidasYUnaValida() {
        respuestaDeLaIa = respuestaIa(
                manual("Cobrá $5.000 más", JUSTIFICACION_ACCION),
                manual(TITULO_ACCION, "Vas a vender un 20 % más"),
                manual(null, null),
                manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void givenTresMetricasYUnaAccion() {
        respuestaDeLaIa = respuestaIa(
                medido(METRICA_C1), medido(METRICA_C2), medido(METRICA_C3),
                manual(TITULO_ACCION, JUSTIFICACION_ACCION));
    }

    private void givenUnaMetaEnPesosConMetricas() {
        respuestaDeLaIa = new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, true, List.of(medido(METRICA_C1)), CONSUMO);
    }

    private void givenUnaRespuestaDeLaIaConTexto(String respuesta) {
        respuestaDeLaIa = new PropuestaObjetivoIa(
                respuesta, ENTENDIDO, false, List.of(), new ConsumoIaSami("p", "m", 1, 1));
    }

    private void givenUnaRespuestaDeLaIaConEntendido(String entendido) {
        respuestaDeLaIa = new PropuestaObjetivoIa(RESPUESTA_IA, entendido, false, List.of(), CONSUMO);
    }

    private void givenUnaAccionYDosMetricas() {
        respuestaDeLaIa = respuestaIa(
                manual(TITULO_ACCION, JUSTIFICACION_ACCION), medido(METRICA_C1), medido(METRICA_C2));
    }

    // --- act ---
    private void whenSeArmaLaPropuesta() {
        propuesta = PropuestaObjetivo.de(respuestaDeLaIa, consulta());
    }

    private void whenSeArmaLaPropuestaConError() {
        error = catchThrowable(this::whenSeArmaLaPropuesta);
    }

    private void whenSeArmaElRespaldo() {
        propuesta = PropuestaObjetivo.respaldo(OBJETIVO);
    }

    // --- assert ---
    private void thenLaPropuestaTomaLosDatosDeLaIa() {
        assertThat(propuesta.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(propuesta.entendido()).isEqualTo(ENTENDIDO);
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(propuesta.esMonetario()).isFalse();
    }

    private void thenLosModosSonMedidoYManual() {
        thenLosModosSon(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
    }

    private void thenLosModosSon(ModoMiniObjetivo... modos) {
        assertThat(propuesta.minis()).extracting(MiniObjetivoPropuesto::modo).containsExactly(modos);
    }

    private void thenLaMetricaEsLaDelCatalogoYLaAccionLaDeLaIa() {
        assertThat(propuesta.minis().get(0).candidato()).isEqualTo(catalogo().get(2));
        assertThat(propuesta.minis().get(1).accion())
                .isEqualTo(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION));
        assertThat(propuesta.minis().get(1).textoConIa()).isTrue();
    }

    private void thenElTipoEs(TipoObjetivo tipo) {
        assertThat(propuesta.tipo()).contains(tipo);
    }

    private void thenSePuedeGuardar() {
        assertThat(propuesta.puedeGuardar()).isTrue();
    }

    private void thenNoSePuedeGuardar() {
        assertThat(propuesta.puedeGuardar()).isFalse();
    }

    private void thenLaUnicaMetricaEsLaDelCatalogoSinTextoDeIa() {
        assertThat(propuesta.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.candidato()).isEqualTo(catalogo().get(1));
            assertThat(mini.textoConIa()).isFalse();
        });
    }

    private void thenLasMetricasSonLasDelCatalogo(int... posiciones) {
        List<CandidatoMiniObjetivo> esperadas = Arrays.stream(posiciones).mapToObj(catalogo()::get).toList();
        assertThat(propuesta.minis()).extracting(MiniObjetivoPropuesto::candidato)
                .containsExactlyElementsOf(esperadas);
    }

    private void thenHayMinis(int cantidad) {
        assertThat(propuesta.minis()).hasSize(cantidad);
    }

    private void thenSoloQuedaLaAccionValida() {
        assertThat(propuesta.minis()).singleElement()
                .satisfies(mini -> assertThat(mini.accion().titulo()).isEqualTo(TITULO_ACCION));
    }

    private void thenQuedanElMaximoDeMinisTodosMedidos() {
        assertThat(propuesta.minis()).hasSize(MiniObjetivo.MAXIMO_EN_CURSO);
        assertThat(propuesta.minis()).allMatch(MiniObjetivoPropuesto::esMedido);
    }

    private void thenLaPropuestaEsMonetariaSinMinisNiTipo() {
        assertThat(propuesta.esMonetario()).isTrue();
        thenNoHayMinisNiTipo();
    }

    private void thenNoHayMinisNiTipo() {
        assertThat(propuesta.minis()).isEmpty();
        assertThat(propuesta.tipo()).isEmpty();
    }

    private void thenLaIaNoSirve() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void thenLaIaNoSirveConElMensajeDeRespuestaInvalida() {
        assertThat(error).isInstanceOf(IllegalStateException.class).hasMessage("La IA no dio una respuesta válida");
    }

    private void thenLaRespuestaTieneElLargoMaximo() {
        assertThat(propuesta.respuesta()).hasSize(LARGO_MAXIMO_DE_RESPUESTA);
    }

    private void thenElEntendidoEsNulo() {
        assertThat(propuesta.entendido()).isNull();
    }

    private void thenElEntendidoEs(String esperado) {
        assertThat(propuesta.entendido()).isEqualTo(esperado);
    }

    private void thenElRespaldoEsUnSeguimientoSemanal() {
        assertThat(propuesta.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(propuesta.entendido()).isNull();
        assertThat(propuesta.esMonetario()).isFalse();
        assertThat(propuesta.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.esManual()).isTrue();
            assertThat(mini.textoConIa()).isFalse();
            assertThat(mini.accion().justificacion()).contains(OBJETIVO);
        });
        thenElTipoEs(TipoObjetivo.SIN_DATOS);
        thenSePuedeGuardar();
    }
}
