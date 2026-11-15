package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.MiniObjetivoTestData.candidato;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ObjetivosIaConfigTest {

    private final ObjetivosIaConfig config = new ObjetivosIaConfig();

    @Test
    @DisplayName("Arma el pedido con las instrucciones y el esquema de los archivos de la IA")
    void promptObjetivosShouldLoadTheResources() {
        PromptObjetivos prompt = config.promptObjetivos();

        assertThat(prompt.peticion(List.of(candidato())).instrucciones()).contains("Sos Sami");
    }

    @Test
    @DisplayName("Con el proveedor simulado conecta el redactor, que no sabe escribir y deja el texto de reserva")
    void redaccionObjetivosIaPortShouldUseTheSimulatedProvider() {
        RedaccionObjetivosIaPort port = config.redaccionObjetivosIaPort(
                config.promptObjetivos(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("simulado", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of(new ProveedorIaSimulado(
                        new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales))));

        assertThat(port).isInstanceOf(RedaccionObjetivosIaAdapter.class);
        assertThatThrownBy(() -> port.redactar(List.of(candidato()))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Con un proveedor que no existe no levanta")
    void redaccionObjetivosIaPortShouldFailWhenTheProviderDoesNotExist() {
        assertThatThrownBy(() -> config.redaccionObjetivosIaPort(
                config.promptObjetivos(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("inexistente", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    // --- helpers ---
    private static SamiProperties sami() {
        return new SamiProperties(0.7, 3, 20, 60,
                new SamiProperties.Ia(null, null, null, null, null, Duration.ofSeconds(30), 700, 0.6, 8000));
    }
}
