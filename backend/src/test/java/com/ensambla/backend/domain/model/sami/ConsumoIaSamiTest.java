package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumoIaSamiTest {

    private ConsumoIaSami total;

    @Test
    @DisplayName("Sumar dos consumos junta los modelos y los tokens")
    void sumarDosConsumosJuntaLosModelosYLosTokens() {
        cuandoSeSuman(new ConsumoIaSami("bedrock", "haiku", 100, 20), new ConsumoIaSami("bedrock", "sonnet", 150, 30));

        entoncesElTotalEs(new ConsumoIaSami("bedrock", "haiku + sonnet", 250, 50));
    }

    private void cuandoSeSuman(ConsumoIaSami primero, ConsumoIaSami segundo) {
        total = primero.sumar(segundo);
    }

    private void entoncesElTotalEs(ConsumoIaSami esperado) {
        assertThat(total).isEqualTo(esperado);
    }
}