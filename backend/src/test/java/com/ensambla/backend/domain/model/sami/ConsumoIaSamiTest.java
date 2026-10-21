package com.ensambla.backend.domain.model.sami;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ConsumoIaSamiTest {

    @Test
    @DisplayName("Sumar dos consumos junta los modelos y los tokens")
    void sumarShouldJoinModelsAndAddTokens() {
        ConsumoIaSami primero = new ConsumoIaSami("bedrock", "haiku", 100, 20);
        ConsumoIaSami segundo = new ConsumoIaSami("bedrock", "sonnet", 150, 30);

        ConsumoIaSami result = primero.sumar(segundo);

        assertThat(result).isEqualTo(new ConsumoIaSami("bedrock", "haiku + sonnet", 250, 50));
    }
}
