package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProveedorIaSimuladoTest {

    private static final AjustesIa AJUSTES =
            new AjustesIa("simulado", null, null, null, null, null, null, Duration.ofSeconds(30));

    private final ProveedorIaSimulado proveedor = new ProveedorIaSimulado(
            new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales);

    @Test
    @DisplayName("Arma las tres puertas sin costo y sin red")
    void proveedorShouldBuildTheThreeSimulatedClients() {
        ClienteModeloVision vision = proveedor.vision(AJUSTES);
        ClienteModeloTexto texto = proveedor.texto(AJUSTES, null, 700);
        ClienteModeloConHerramientas conHerramientas = proveedor.conHerramientas(AJUSTES, 900, List.of());

        assertThat(proveedor.nombre()).isEqualTo("simulado");
        assertThat(vision).isInstanceOf(ClienteModeloVisionSimulado.class);
        assertThat(texto).isInstanceOf(ClienteModeloTextoSimulado.class);
        assertThat(conHerramientas).isInstanceOf(ClienteModeloConHerramientasSimulado.class);
    }
}
