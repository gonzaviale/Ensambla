package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.JUSTIFICACION;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.OTRO_MINI_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO;
import static com.ensambla.backend.support.MiniObjetivoTestData.PRODUCTO_ID;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.VALOR_INICIAL;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.MiniObjetivoTestData.miniObjetivo;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.NEGOCIO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MiniObjetivoTest {

    private static final OffsetDateTime DESPUES = AHORA.plusDays(2);

    @Test
    @DisplayName("Uno nuevo arranca en curso con el valor inicial del candidato y un id propio")
    void nuevoShouldStartInProgressWithTheCandidateData() {
        MiniObjetivo result = MiniObjetivo.nuevo(objetivo(), 2, candidato(), SEMANA, AHORA);

        assertThat(result.getId()).isNotNull();
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.getNegocioId()).isEqualTo(NEGOCIO_ID);
        assertThat(result.getPosicion()).isEqualTo(2);
        assertThat(result.getTipo()).isEqualTo(TipoMiniObjetivo.SUBIR_PRECIO);
        assertThat(result.getRegistroId()).isEqualTo(PRODUCTO_ID);
        assertThat(result.getReferencia()).isEqualTo(PRODUCTO);
        assertThat(result.getMeta()).isEqualTo(META_MINI);
        assertThat(result.getValorInicial()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getValorActual()).isEqualTo(VALOR_INICIAL);
        assertThat(result.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(result.getEstado()).isEqualTo(EstadoMiniObjetivo.EN_CURSO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
        assertThat(result.getTitulo()).isEqualTo(TITULO);
        assertThat(result.getJustificacion()).isEqualTo(JUSTIFICACION);
        assertThat(result.isTextoConIa()).isFalse();
        assertThat(result.getReemplazadoPor()).isNull();
        assertThat(result.getCreadoEn()).isEqualTo(AHORA);
        assertThat(result.getActualizadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("Uno nuevo conserva que el texto lo redactó la IA")
    void nuevoShouldKeepThatTheTextCameFromTheAi() {
        CandidatoMiniObjetivo conIa = candidato().conTextoDeIa(new TextoMiniObjetivo("Título", "Razón"));

        MiniObjetivo result = MiniObjetivo.nuevo(objetivo(), 1, conIa, SEMANA, AHORA);

        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getTitulo()).isEqualTo("Título");
    }

    @Test
    @DisplayName("Está en curso mientras no se logre, se reemplace o se quite")
    void estaEnCursoShouldDependOnTheState() {
        assertThat(miniObjetivo().estaEnCurso()).isTrue();
        assertThat(conEstado(EstadoMiniObjetivo.LOGRADO).estaEnCurso()).isFalse();
    }

    @Test
    @DisplayName("Un mini-objetivo que sube se cumple cuando el valor llega a la meta o la pasa")
    void cumplidoShouldBeTrueWhenAnUpwardValueReachesTheGoal() {
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "14182.99").cumplido()).isFalse();
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "14183.00").cumplido()).isTrue();
        assertThat(conValorActual(TipoMiniObjetivo.SUBIR_PRECIO, "20000.00").cumplido()).isTrue();
    }

    @Test
    @DisplayName("Un mini-objetivo que baja se cumple cuando el valor baja hasta la meta o por debajo")
    void cumplidoShouldBeTrueWhenADownwardValueReachesTheGoal() {
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "14183.01").cumplido()).isFalse();
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "14183.00").cumplido()).isTrue();
        assertThat(conValorActual(TipoMiniObjetivo.RECORTAR_GASTO, "100.00").cumplido()).isTrue();
    }

    @Test
    @DisplayName("El avance de uno que sube es la parte del camino de lo inicial a la meta que ya se recorrió")
    void avanceShouldBeTheShareOfTheWayFromTheInitialValueToTheGoal() {
        MiniObjetivo mitad = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "150.00");

        assertThat(mitad.avance()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("El avance de uno que baja cuenta lo que bajó del valor inicial")
    void avanceShouldCountWhatAValueThatShouldGoDownAlreadyWentDown() {
        MiniObjetivo mitad = conValores(TipoMiniObjetivo.RECORTAR_GASTO, "100.00", "60.00", "80.00");

        assertThat(mitad.avance()).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("El avance no baja de 0 si el valor se aleja de la meta")
    void avanceShouldNotGoBelowZero() {
        MiniObjetivo peor = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "50.00");

        assertThat(peor.avance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("El avance no pasa de 1 si el valor supera la meta")
    void avanceShouldNotGoAboveOne() {
        MiniObjetivo superado = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "100.00", "200.00", "500.00");

        assertThat(superado.avance()).isEqualByComparingTo("1");
    }

    @Test
    @DisplayName("Sin camino que recorrer, el avance es 1 si ya se cumple y 0 si no")
    void avanceShouldBeOneOrZeroWhenThereIsNoWayToGo() {
        MiniObjetivo cumplido = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "200.00");
        MiniObjetivo sinCumplir = conValores(TipoMiniObjetivo.SUBIR_PRECIO, "200.00", "200.00", "150.00");

        assertThat(cumplido.avance()).isEqualByComparingTo("1");
        assertThat(sinCumplir.avance()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("Al reemplazarlo queda reemplazado, apunta al nuevo y se anota cuándo")
    void reemplazarPorShouldMarkItAsReplaced() {
        MiniObjetivo mini = miniObjetivo();

        mini.reemplazarPor(OTRO_MINI_ID, DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.REEMPLAZADO);
        assertThat(mini.getReemplazadoPor()).isEqualTo(OTRO_MINI_ID);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("Al quitarlo queda abandonado y se anota cuándo")
    void abandonarShouldMarkItAsAbandoned() {
        MiniObjetivo mini = miniObjetivo();

        mini.abandonar(DESPUES);

        assertThat(mini.getEstado()).isEqualTo(EstadoMiniObjetivo.ABANDONADO);
        assertThat(mini.getActualizadoEn()).isEqualTo(DESPUES);
    }

    @Test
    @DisplayName("No se puede reemplazar uno que ya no está en curso")
    void reemplazarPorShouldFailWhenItIsNotInProgress() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.LOGRADO);

        assertThatThrownBy(() -> mini.reemplazarPor(OTRO_MINI_ID, DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    @Test
    @DisplayName("No se puede quitar uno que ya no está en curso")
    void abandonarShouldFailWhenItIsNotInProgress() {
        MiniObjetivo mini = conEstado(EstadoMiniObjetivo.REEMPLAZADO);

        assertThatThrownBy(() -> mini.abandonar(DESPUES))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("El mini-objetivo ya no está en curso");
    }

    // --- helpers ---
    private static MiniObjetivo conEstado(EstadoMiniObjetivo estado) {
        MiniObjetivo mini = miniObjetivo();
        mini.setEstado(estado);
        return mini;
    }

    private static MiniObjetivo conValorActual(TipoMiniObjetivo tipo, String valorActual) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setValorActual(new BigDecimal(valorActual));
        return mini;
    }

    private static MiniObjetivo conValores(TipoMiniObjetivo tipo, String inicial, String meta, String actual) {
        MiniObjetivo mini = miniObjetivo();
        mini.setTipo(tipo);
        mini.setValorInicial(new BigDecimal(inicial));
        mini.setMeta(new BigDecimal(meta));
        mini.setValorActual(new BigDecimal(actual));
        return mini;
    }
}
