package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropuestaObjetivoTest {

    @Test
    @DisplayName("Mezcla una métrica del catálogo con una acción: el objetivo sería híbrido")
    void deShouldMixAMeasuredAndAManualMini() {
        PropuestaObjetivo result = PropuestaObjetivo.de(
                respuestaIa(medido("c3"), manual(TITULO_ACCION, JUSTIFICACION_ACCION)), consulta());

        assertThat(result.respuesta()).isEqualTo(RESPUESTA_IA);
        assertThat(result.entendido()).isEqualTo(ENTENDIDO);
        assertThat(result.origen()).isEqualTo(OrigenPropuesta.IA);
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.minis()).extracting(MiniObjetivoPropuesto::modo)
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(result.minis().get(0).candidato()).isEqualTo(catalogo().get(2));
        assertThat(result.minis().get(1).accion()).isEqualTo(new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION));
        assertThat(result.minis().get(1).textoConIa()).isTrue();
        assertThat(result.tipo()).contains(TipoObjetivo.HIBRIDO);
        assertThat(result.puedeGuardar()).isTrue();
    }

    @Test
    @DisplayName("Una métrica medida usa el texto y los números del servidor, nunca los de la IA")
    void deShouldUseTheServerNumbersForAMeasuredMini() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(medido("c2")), consulta());

        assertThat(result.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.candidato()).isEqualTo(catalogo().get(1));
            assertThat(mini.textoConIa()).isFalse();
        });
        assertThat(result.tipo()).contains(TipoObjetivo.CON_DATOS);
    }

    @Test
    @DisplayName("Una métrica fuera del catálogo se descarta")
    void deShouldDiscardAMetricOutsideTheCatalog() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(medido("c9"), medido("c1")), consulta());

        assertThat(result.minis()).extracting(MiniObjetivoPropuesto::candidato).containsExactly(catalogo().get(0));
    }

    @Test
    @DisplayName("Dos métricas que miden lo mismo (mismo producto) se quedan en la primera")
    void deShouldDiscardTheMetricsThatClash() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(medido("c1"), medido("c4"), medido("c2")),
                consulta());

        assertThat(result.minis()).extracting(MiniObjetivoPropuesto::candidato)
                .containsExactly(catalogo().get(0), catalogo().get(1));
    }

    @Test
    @DisplayName("La misma métrica dos veces se queda con una sola")
    void deShouldDiscardARepeatedMetric() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(medido("c2"), medido("c2")), consulta());

        assertThat(result.minis()).hasSize(1);
    }

    @Test
    @DisplayName("Una acción con un importe o un porcentaje, o sin texto, se descarta")
    void deShouldDiscardAnInvalidAction() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(
                manual("Cobrá $5.000 más", JUSTIFICACION_ACCION),
                manual(TITULO_ACCION, "Vas a vender un 20 % más"),
                manual(null, null),
                manual(TITULO_ACCION, JUSTIFICACION_ACCION)), consulta());

        assertThat(result.minis()).singleElement()
                .satisfies(mini -> assertThat(mini.accion().titulo()).isEqualTo(TITULO_ACCION));
    }

    @Test
    @DisplayName("Se quedan los primeros 3 mini-objetivos")
    void deShouldKeepAtMostThree() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(
                medido("c1"), medido("c2"), medido("c3"), manual(TITULO_ACCION, JUSTIFICACION_ACCION)), consulta());

        assertThat(result.minis()).hasSize(MiniObjetivo.MAXIMO_EN_CURSO);
        assertThat(result.minis()).allMatch(MiniObjetivoPropuesto::esMedido);
    }

    @Test
    @DisplayName("Una meta en pesos no arma mini-objetivos aunque la IA los mande, y no se puede guardar")
    void deShouldNotProposeMinisForAMonetaryGoal() {
        PropuestaObjetivoIa ia = new PropuestaObjetivoIa(RESPUESTA_IA, ENTENDIDO, true, List.of(medido("c1")), CONSUMO);

        PropuestaObjetivo result = PropuestaObjetivo.de(ia, consulta());

        assertThat(result.esMonetario()).isTrue();
        assertThat(result.minis()).isEmpty();
        assertThat(result.tipo()).isEmpty();
        assertThat(result.puedeGuardar()).isFalse();
    }

    @Test
    @DisplayName("Si no queda nada válido para guardar no hay tipo ni se puede guardar")
    void deShouldNotAllowSavingWithoutMinis() {
        PropuestaObjetivo result = PropuestaObjetivo.de(respuestaIa(medido("c9")), consulta());

        assertThat(result.minis()).isEmpty();
        assertThat(result.tipo()).isEmpty();
        assertThat(result.puedeGuardar()).isFalse();
    }

    @Test
    @DisplayName("Sin respuesta para la persona, o una de más de 600 caracteres, la IA no sirve")
    void deShouldRejectAMissingOrTooLongAnswer() {
        assertThatThrownBy(() -> PropuestaObjetivo.de(conRespuesta(null), consulta()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PropuestaObjetivo.de(conRespuesta("  "), consulta()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> PropuestaObjetivo.de(conRespuesta("a".repeat(601)), consulta()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("La IA no dio una respuesta válida");
    }

    @Test
    @DisplayName("Una respuesta de 600 caracteres se acepta y queda sin espacios en los extremos")
    void deShouldAcceptAnAnswerOfTheMaximumLength() {
        assertThat(PropuestaObjetivo.de(conRespuesta(" " + "a".repeat(600) + " "), consulta()).respuesta())
                .hasSize(600);
    }

    @Test
    @DisplayName("Un «entendido» vacío, en blanco o de más de 300 caracteres queda sin entender")
    void deShouldDropAnInvalidUnderstanding() {
        assertThat(conEntendido(null).entendido()).isNull();
        assertThat(conEntendido("").entendido()).isNull();
        assertThat(conEntendido("a".repeat(301)).entendido()).isNull();
        assertThat(conEntendido("  " + ENTENDIDO + " ").entendido()).isEqualTo(ENTENDIDO);
    }

    @Test
    @DisplayName("El respaldo es un solo mini-objetivo manual de seguimiento con el objetivo de la persona")
    void respaldoShouldBeAWeeklyFollowUp() {
        PropuestaObjetivo result = PropuestaObjetivo.respaldo(OBJETIVO);

        assertThat(result.origen()).isEqualTo(OrigenPropuesta.RESPALDO);
        assertThat(result.entendido()).isNull();
        assertThat(result.esMonetario()).isFalse();
        assertThat(result.minis()).singleElement().satisfies(mini -> {
            assertThat(mini.esManual()).isTrue();
            assertThat(mini.textoConIa()).isFalse();
            assertThat(mini.accion().justificacion()).contains(OBJETIVO);
        });
        assertThat(result.tipo()).contains(TipoObjetivo.SIN_DATOS);
        assertThat(result.puedeGuardar()).isTrue();
    }

    // --- helpers ---
    private static PropuestaObjetivoIa conRespuesta(String respuesta) {
        return new PropuestaObjetivoIa(respuesta, ENTENDIDO, false, List.of(), new ConsumoIaSami("p", "m", 1, 1));
    }

    private static PropuestaObjetivo conEntendido(String entendido) {
        return PropuestaObjetivo.de(
                new PropuestaObjetivoIa(RESPUESTA_IA, entendido, false, List.of(), CONSUMO), consulta());
    }

    @Test
    @DisplayName("Una acción elegida antes no impide elegir después una métrica del catálogo")
    void deShouldKeepAMeasuredMetricAfterAManualAction() {
        PropuestaObjetivo result = PropuestaObjetivo.de(
                respuestaIa(manual(TITULO_ACCION, JUSTIFICACION_ACCION), medido("c1"), medido("c2")), consulta());

        assertThat(result.minis()).extracting(MiniObjetivoPropuesto::modo)
                .containsExactly(ModoMiniObjetivo.MANUAL, ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MEDIDO);
    }
}
