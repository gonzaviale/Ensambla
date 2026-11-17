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
    void tipoShouldMatchEachMotivo(MotivoReajuste motivoDelReajuste, TipoReajuste esperado) {
        givenUnMotivo(motivoDelReajuste);

        whenSeClasificaElReajuste();

        thenElReajusteEs(esperado);
    }

    // --- arrange ---
    private void givenUnMotivo(MotivoReajuste motivoDelReajuste) {
        motivo = motivoDelReajuste;
    }

    // --- act ---
    private void whenSeClasificaElReajuste() {
        tipo = motivo.tipo();
    }

    // --- assert ---
    private void thenElReajusteEs(TipoReajuste esperado) {
        assertThat(tipo).isEqualTo(esperado);
    }
}
