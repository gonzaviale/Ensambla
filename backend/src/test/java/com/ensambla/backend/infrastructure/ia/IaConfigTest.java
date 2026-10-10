package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IaConfigTest {

    private static final IaProperties PROPERTIES =
            new IaProperties("simulado", null, null, Duration.ofSeconds(60), 10, 0.8);

    private IaConfig config;

    @BeforeEach
    void setUp() {
        config = new IaConfig();
    }

    @Test
    @DisplayName("Con el proveedor simulado, el lector de cuadernos devuelve la lectura de ejemplo validada")
    void lectorCuadernoShouldReadExampleWithSimulatedProvider() {
        LectorCuadernoPort lector = config.lectorCuaderno(config.clienteModeloVisionSimulado(),
                config.promptLecturaCuaderno(), JsonMapper.builder().build(), PROPERTIES);

        LecturaCuaderno lectura = lector.leer(List.of(new PaginaCuaderno(1, new byte[]{1}, "image/jpeg")), List.of());

        thenExampleLecturaIsValid(lectura);
    }

    // --- assert ---
    private void thenExampleLecturaIsValid(LecturaCuaderno lectura) {
        assertThat(lectura.bloque().nombre()).isEqualTo("Ventas");
        assertThat(lectura.bloque().campos()).hasSize(6);
        assertThat(lectura.filas()).hasSize(3);
        assertThat(lectura.filas()).filteredOn(fila -> fila.tieneDudas(PROPERTIES.umbralConfianza())).hasSize(2);
        assertThat(lectura.advertencias()).hasSize(1);
        assertThat(lectura.consumo().proveedor()).isEqualTo(ClienteModeloVisionSimulado.PROVEEDOR);
    }
}
