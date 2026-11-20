package com.ensambla.backend.domain.model.objetivo;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class MotivoReajusteTest {

    private MotivoReajuste motivo;
    private TipoReajuste tipo;

    @ParameterizedTest
    @CsvSource({
            "LOGRADO, SUBIR_META",
            "DECISION_SIN_EFECTO, PROBAR_OTRO_CAMINO",
            "QUITADO, REEMPLAZAR",
            "PRODUCTO_FUERA, REEMPLAZAR",
            "SIN_AVANCE, REEMPLAZAR"})
    @DisplayName("Lo logrado sube la meta, la decisión sin efecto prueba otro camino y lo demás se reemplaza")
    void loLogradoSubeLaMetaLaDecisionSinEfectoPruebaOtroCaminoYLoDemasSeReemplaza(MotivoReajuste motivoDelReajuste, TipoReajuste esperado) {
        dadoUnMotivo(motivoDelReajuste);

        cuandoSeClasificaElReajuste();

        entoncesElReajusteEs(esperado);
    }

    // --- arrange ---
    private void dadoUnMotivo(MotivoReajuste motivoDelReajuste) {
        motivo = motivoDelReajuste;
    }

    // --- act ---
    private void cuandoSeClasificaElReajuste() {
        tipo = motivo.tipo();
    }

    // --- assert ---
    private void entoncesElReajusteEs(TipoReajuste esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }
}
