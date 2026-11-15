package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.objetivo.AccionRenovacion;
import com.ensambla.backend.domain.model.objetivo.HizoManual;
import com.ensambla.backend.domain.model.objetivo.MiniObjetivoPropuesto;
import com.ensambla.backend.domain.model.objetivo.ModoMiniObjetivo;
import com.ensambla.backend.domain.model.objetivo.PedidoRenovacion;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivo;
import com.ensambla.backend.domain.model.objetivo.PropuestaObjetivoIa;
import com.ensambla.backend.domain.model.objetivo.QuePaso;
import com.ensambla.backend.domain.model.objetivo.RedaccionObjetivosIa;
import com.ensambla.backend.domain.port.ConversacionObjetivosIaPort;
import com.ensambla.backend.domain.port.RedaccionObjetivosIaPort;
import com.ensambla.backend.domain.port.RenovacionManualesIaPort;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.ensambla.backend.support.ConversacionObjetivoTestData.consulta;
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

    @Test
    @DisplayName("Arma el pedido de la conversación con las instrucciones y el esquema de los archivos de la IA")
    void promptConversacionObjetivosShouldLoadTheResources() {
        PromptConversacionObjetivos prompt = config.promptConversacionObjetivos();

        assertThat(prompt.peticion(consulta()).instrucciones()).contains("Sos Sami");
    }

    @Test
    @DisplayName("Con el proveedor simulado la conversación propone una métrica del catálogo y una acción, sin costo")
    void conversacionObjetivosIaPortShouldUseTheSimulatedProvider() {
        ConversacionObjetivosIaPort port = config.conversacionObjetivosIaPort(
                config.promptConversacionObjetivos(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("simulado", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of(new ProveedorIaSimulado(
                        new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales))));

        PropuestaObjetivoIa result = port.conversar(consulta());

        assertThat(port).isInstanceOf(ConversacionObjetivosIaAdapter.class);
        assertThat(PropuestaObjetivo.de(result, consulta()).minis())
                .extracting(MiniObjetivoPropuesto::modo)
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(result.consumo().tokensEntrada()).isZero();
    }

    @Test
    @DisplayName("Con un proveedor que no existe la conversación no levanta")
    void conversacionObjetivosIaPortShouldFailWhenTheProviderDoesNotExist() {
        assertThatThrownBy(() -> config.conversacionObjetivosIaPort(
                config.promptConversacionObjetivos(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("inexistente", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("Arma el pedido de las renovaciones con las instrucciones y el esquema de los archivos de la IA")
    void promptRenovacionManualesShouldLoadTheResources() {
        PromptRenovacionManuales prompt = config.promptRenovacionManuales();

        assertThat(prompt.peticion(List.of(pedidoDeRenovacion())).instrucciones()).contains("Sos Sami");
    }

    @Test
    @DisplayName("Con el proveedor simulado las renovaciones tienen un texto válido por cada una, sin costo")
    void renovacionManualesIaPortShouldUseTheSimulatedProvider() {
        RenovacionManualesIaPort port = config.renovacionManualesIaPort(
                config.promptRenovacionManuales(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("simulado", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of(new ProveedorIaSimulado(
                        new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales))));

        RedaccionObjetivosIa result = port.renovar(List.of(pedidoDeRenovacion(), pedidoDeRenovacion()));

        assertThat(port).isInstanceOf(RenovacionManualesIaAdapter.class);
        assertThat(result.textos()).hasSize(2);
        assertThat(result.consumo().tokensEntrada()).isZero();
    }

    @Test
    @DisplayName("Con un proveedor que no existe las renovaciones no levantan")
    void renovacionManualesIaPortShouldFailWhenTheProviderDoesNotExist() {
        assertThatThrownBy(() -> config.renovacionManualesIaPort(
                config.promptRenovacionManuales(),
                JsonMapper.builder().build(),
                sami(),
                new IaProperties("inexistente", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of())))
                .isInstanceOf(IllegalStateException.class);
    }

    private static PedidoRenovacion pedidoDeRenovacion() {
        return new PedidoRenovacion(AccionRenovacion.SE_DIVIDE, "Un cartel", "Que te vean", HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO, null);
    }
}
