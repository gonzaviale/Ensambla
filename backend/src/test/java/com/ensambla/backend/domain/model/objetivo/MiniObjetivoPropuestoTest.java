package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.MiniObjetivoTestData.APORTE;
import static com.ensambla.backend.support.MiniObjetivoTestData.META_MINI;
import static com.ensambla.backend.support.MiniObjetivoTestData.SEMANA;
import static com.ensambla.backend.support.MiniObjetivoTestData.TITULO;
import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.JUSTIFICACION_ACCION;
import static com.ensambla.backend.support.ConversacionObjetivoTestData.TITULO_ACCION;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static com.ensambla.backend.support.ObjetivoTestData.OBJETIVO_ID;
import static com.ensambla.backend.support.ObjetivoTestData.objetivo;
import static org.assertj.core.api.Assertions.assertThat;

class MiniObjetivoPropuestoTest {

    @Test
    @DisplayName("Uno medido lleva la métrica del catálogo y el texto del servidor")
    void medidoShouldCarryTheCatalogMetric() {
        MiniObjetivoPropuesto result = MiniObjetivoPropuesto.medido(candidato());

        assertThat(result.modo()).isEqualTo(ModoMiniObjetivo.MEDIDO);
        assertThat(result.esMedido()).isTrue();
        assertThat(result.esManual()).isFalse();
        assertThat(result.candidato()).isEqualTo(candidato());
        assertThat(result.accion()).isNull();
        assertThat(result.textoConIa()).isFalse();
    }

    @Test
    @DisplayName("Uno manual lleva la acción y si la redactó la IA")
    void manualShouldCarryTheAction() {
        MiniObjetivoPropuesto result = MiniObjetivoPropuesto.manual(accion(), true);

        assertThat(result.modo()).isEqualTo(ModoMiniObjetivo.MANUAL);
        assertThat(result.esManual()).isTrue();
        assertThat(result.esMedido()).isFalse();
        assertThat(result.accion()).isEqualTo(accion());
        assertThat(result.candidato()).isNull();
        assertThat(result.textoConIa()).isTrue();
    }

    @Test
    @DisplayName("Uno medido se vuelve un mini-objetivo medido en curso con los números del candidato")
    void aMiniObjetivoShouldBuildAMeasuredMini() {
        MiniObjetivo result = MiniObjetivoPropuesto.medido(candidato()).aMiniObjetivo(objetivo(), 2, SEMANA, AHORA);

        assertThat(result.esMedido()).isTrue();
        assertThat(result.getObjetivoId()).isEqualTo(OBJETIVO_ID);
        assertThat(result.getPosicion()).isEqualTo(2);
        assertThat(result.getMeta()).isEqualTo(META_MINI);
        assertThat(result.getAporteEstimado()).isEqualTo(APORTE);
        assertThat(result.getTitulo()).isEqualTo(TITULO);
        assertThat(result.getSemanaInicio()).isEqualTo(SEMANA);
    }

    @Test
    @DisplayName("Uno manual se vuelve un mini-objetivo manual en curso sin números")
    void aMiniObjetivoShouldBuildAManualMini() {
        MiniObjetivo result = MiniObjetivoPropuesto.manual(accion(), true).aMiniObjetivo(objetivo(), 3, SEMANA, AHORA);

        assertThat(result.esManual()).isTrue();
        assertThat(result.getPosicion()).isEqualTo(3);
        assertThat(result.getTitulo()).isEqualTo(TITULO_ACCION);
        assertThat(result.getJustificacion()).isEqualTo(JUSTIFICACION_ACCION);
        assertThat(result.isTextoConIa()).isTrue();
        assertThat(result.getMeta()).isNull();
        assertThat(result.getTipo()).isNull();
    }

    private static AccionManual accion() {
        return new AccionManual(TITULO_ACCION, JUSTIFICACION_ACCION);
    }
}
