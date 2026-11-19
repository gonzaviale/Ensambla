package com.ensambla.backend.domain.model.objetivo;

import com.ensambla.backend.domain.model.aviso.Aviso;
import com.ensambla.backend.domain.model.aviso.EstadoAviso;
import com.ensambla.backend.domain.model.aviso.TipoAviso;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.ensambla.backend.support.DecisionTestData.AVISO_ID;
import static com.ensambla.backend.support.DecisionTestData.aviso;
import static com.ensambla.backend.support.DecisionTestData.decision;
import static com.ensambla.backend.support.DecisionTestData.impacto;
import static com.ensambla.backend.support.ObjetivoTestData.AHORA;
import static org.assertj.core.api.Assertions.assertThat;

class EfectoDecisionTest {

    private Aviso aviso;
    private EfectoDecision efecto;

    @Test
    @DisplayName("Un aviso aplicado con el impacto ya medido trae lo que cambió el ingreso semanal")
    void unAvisoAplicadoConElImpactoYaMedidoTraeLoQueCambioElIngresoSemanal() {
        dadoUnAvisoAplicadoConElImpactoMedido();

        cuandoSeMideElEfectoDeAplicarlo();

        entoncesElEfectoEstaMedidoConElCambioDeIngreso();
    }

    @Test
    @DisplayName("Un aviso aplicado de los que se miden, que todavía no terminó la ventana, está midiendo")
    void unAvisoAplicadoDeLosQueSeMidenQueTodaviaNoTerminoLaVentanaEstaMidiendo() {
        dadoUnAvisoAplicadoDe(TipoAviso.ESTANCADO);

        cuandoSeMideElEfectoDeAplicarlo();

        entoncesElEfectoEstaMidiendoSinCambioDeIngreso();
    }

    @Test
    @DisplayName("Un aviso aplicado de un tipo que no se mide queda sin medición")
    void unAvisoAplicadoDeUnTipoQueNoSeMideQuedaSinMedicion() {
        dadoUnAvisoAplicadoDe(TipoAviso.COMBO);

        cuandoSeMideElEfectoDeAplicarlo();

        entoncesElEfectoQuedaSinMedicion();
    }

    @Test
    @DisplayName("Si el aviso ya no existe no hay con qué medir el efecto")
    void siElAvisoYaNoExisteNoHayConQueMedirElEfecto() {
        cuandoSeMideElEfectoDeAplicarUnAvisoBorrado();

        entoncesElEfectoQuedaSinMedicion();
    }

    @Test
    @DisplayName("Un aviso descartado no se mide")
    void unAvisoDescartadoNoSeMide() {
        dadoUnAvisoDescartado();

        cuandoSeMideElEfectoDeDescartarlo();

        entoncesElEfectoQuedaSinMedicion();
    }

    @Test
    @DisplayName("El paso de un mini-objetivo no se mide")
    void elPasoDeUnMiniObjetivoNoSeMide() {
        cuandoSeMideElEfectoDeAceptarUnPaso();

        entoncesElEfectoQuedaSinMedicion();
    }

    // --- dado ---

    private void dadoUnAvisoAplicadoConElImpactoMedido() {
        dadoUnAvisoAplicadoDe(TipoAviso.ESTANCADO);
        aviso.registrarImpacto(impacto(), AHORA);
    }

    private void dadoUnAvisoAplicadoDe(TipoAviso tipo) {
        aviso = aviso(tipo, EstadoAviso.APLICADO);
    }

    private void dadoUnAvisoDescartado() {
        aviso = aviso(TipoAviso.ESTANCADO, EstadoAviso.DESCARTADO);
    }

    // --- cuando ---

    private void cuandoSeMideElEfectoDeAplicarlo() {
        efecto = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), aviso);
    }

    private void cuandoSeMideElEfectoDeAplicarUnAvisoBorrado() {
        efecto = EfectoDecision.de(decision(TipoDecision.AVISO_APLICADO, AVISO_ID), null);
    }

    private void cuandoSeMideElEfectoDeDescartarlo() {
        efecto = EfectoDecision.de(decision(TipoDecision.AVISO_DESCARTADO, AVISO_ID), aviso);
    }

    private void cuandoSeMideElEfectoDeAceptarUnPaso() {
        efecto = EfectoDecision.de(decision(TipoDecision.PASO_ACEPTADO, null), null);
    }

    // --- entonces ---

    private void entoncesElEfectoEstaMedidoConElCambioDeIngreso() {
        assertThat(efecto.estado()).isEqualTo(EstadoEfecto.MEDIDO);
        assertThat(efecto.deltaIngresoSemanal()).isEqualByComparingTo("350.00");
    }

    private void entoncesElEfectoEstaMidiendoSinCambioDeIngreso() {
        assertThat(efecto.estado()).isEqualTo(EstadoEfecto.MIDIENDO);
        assertThat(efecto.deltaIngresoSemanal()).isNull();
    }

    private void entoncesElEfectoQuedaSinMedicion() {
        assertThat(efecto).isEqualTo(EfectoDecision.SIN_MEDICION);
    }
}
