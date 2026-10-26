package com.ensambla.backend.domain.useCase.sami.crearProyectoDesdeSami;

import com.ensambla.backend.domain.model.campo.ConfiguracionCampo;
import com.ensambla.backend.domain.useCase.sami.ColumnaSamiItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CrearProyectoDesdeSamiRequestTest {

    @Test
    @DisplayName("Convierte las columnas aceptadas en configuraciones de campo opcionales y sin valor inicial")
    void deShouldBuildFieldConfigurations() {
        List<ConfiguracionCampo> result = new CrearProyectoDesdeSamiRequest(null, null, null, List.of(
                new ColumnaSamiItem("Forma de pago", "OPCION", List.of("Efectivo", "Tarjeta")),
                new ColumnaSamiItem("Saldo", "NUMERO", null))).columnasExtra();

        assertThat(result).containsExactly(
                new ConfiguracionCampo("Forma de pago", "OPCION", false, null, List.of("Efectivo", "Tarjeta"), null, null),
                new ConfiguracionCampo("Saldo", "NUMERO", false, null, null, null, null));
    }
}
