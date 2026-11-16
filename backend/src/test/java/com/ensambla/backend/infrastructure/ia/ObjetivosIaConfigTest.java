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
import static org.assertj.core.api.Assertions.catchThrowable;

class ObjetivosIaConfigTest {

    private final ObjetivosIaConfig config = new ObjetivosIaConfig();
    private PromptObjetivos promptObjetivos;
    private PromptConversacionObjetivos promptConversacion;
    private PromptRenovacionManuales promptRenovacion;
    private RedaccionObjetivosIaPort redaccion;
    private ConversacionObjetivosIaPort conversacion;
    private RenovacionManualesIaPort renovacion;
    private PropuestaObjetivoIa propuesta;
    private RedaccionObjetivosIa textosRenovados;
    private Throwable error;

    @Test
    @DisplayName("Arma el pedido con las instrucciones y el esquema de los archivos de la IA")
    void promptObjetivosShouldLoadTheResources() {
        whenSeCarganLosArchivosDelPromptDeObjetivos();

        thenElPedidoDeObjetivosTieneLasInstrucciones();
    }

    @Test
    @DisplayName("Con el proveedor simulado conecta el redactor, que no sabe escribir y deja el texto de reserva")
    void redaccionObjetivosIaPortShouldUseTheSimulatedProvider() {
        whenSeConectaElRedactorConProveedorSimulado();

        thenElRedactorEsElAdaptadorYNoSabeEscribir();
    }

    @Test
    @DisplayName("Con un proveedor que no existe no levanta")
    void redaccionObjetivosIaPortShouldFailWhenTheProviderDoesNotExist() {
        whenSeConectaElRedactorConProveedorInexistente();

        thenNoLevanta();
    }

    @Test
    @DisplayName("Arma el pedido de la conversación con las instrucciones y el esquema de los archivos de la IA")
    void promptConversacionObjetivosShouldLoadTheResources() {
        whenSeCarganLosArchivosDelPromptDeConversacion();

        thenElPedidoDeConversacionTieneLasInstrucciones();
    }

    @Test
    @DisplayName("Con el proveedor simulado la conversación propone una métrica del catálogo y una acción, sin costo")
    void conversacionObjetivosIaPortShouldUseTheSimulatedProvider() {
        whenSeConectaLaConversacionConProveedorSimulado();

        thenLaConversacionProponeUnaMetricaYUnaAccionSinCosto();
    }

    @Test
    @DisplayName("Con un proveedor que no existe la conversación no levanta")
    void conversacionObjetivosIaPortShouldFailWhenTheProviderDoesNotExist() {
        whenSeConectaLaConversacionConProveedorInexistente();

        thenNoLevanta();
    }

    @Test
    @DisplayName("Arma el pedido de las renovaciones con las instrucciones y el esquema de los archivos de la IA")
    void promptRenovacionManualesShouldLoadTheResources() {
        whenSeCarganLosArchivosDelPromptDeRenovacion();

        thenElPedidoDeRenovacionTieneLasInstrucciones();
    }

    @Test
    @DisplayName("Con el proveedor simulado las renovaciones tienen un texto válido por cada una, sin costo")
    void renovacionManualesIaPortShouldUseTheSimulatedProvider() {
        whenSeConectaLaRenovacionConProveedorSimulado();

        thenLaRenovacionTieneUnTextoPorCadaUnaSinCosto();
    }

    @Test
    @DisplayName("Con un proveedor que no existe las renovaciones no levantan")
    void renovacionManualesIaPortShouldFailWhenTheProviderDoesNotExist() {
        whenSeConectaLaRenovacionConProveedorInexistente();

        thenNoLevanta();
    }

    // --- act ---
    private void whenSeCarganLosArchivosDelPromptDeObjetivos() {
        promptObjetivos = config.promptObjetivos();
    }

    private void whenSeCarganLosArchivosDelPromptDeConversacion() {
        promptConversacion = config.promptConversacionObjetivos();
    }

    private void whenSeCarganLosArchivosDelPromptDeRenovacion() {
        promptRenovacion = config.promptRenovacionManuales();
    }

    private void whenSeConectaElRedactorConProveedorSimulado() {
        redaccion = config.redaccionObjetivosIaPort(config.promptObjetivos(), JsonMapper.builder().build(), sami(),
                propiedadesDelProveedor("simulado"), proveedoresSimulados());
    }

    private void whenSeConectaElRedactorConProveedorInexistente() {
        error = catchThrowable(() -> config.redaccionObjetivosIaPort(config.promptObjetivos(),
                JsonMapper.builder().build(), sami(), propiedadesDelProveedor("inexistente"),
                new ProveedoresIa(List.of())));
    }

    private void whenSeConectaLaConversacionConProveedorSimulado() {
        conversacion = config.conversacionObjetivosIaPort(config.promptConversacionObjetivos(),
                JsonMapper.builder().build(), sami(), propiedadesDelProveedor("simulado"), proveedoresSimulados());
        propuesta = conversacion.conversar(consulta());
    }

    private void whenSeConectaLaConversacionConProveedorInexistente() {
        error = catchThrowable(() -> config.conversacionObjetivosIaPort(config.promptConversacionObjetivos(),
                JsonMapper.builder().build(), sami(), propiedadesDelProveedor("inexistente"),
                new ProveedoresIa(List.of())));
    }

    private void whenSeConectaLaRenovacionConProveedorSimulado() {
        renovacion = config.renovacionManualesIaPort(config.promptRenovacionManuales(),
                JsonMapper.builder().build(), sami(), propiedadesDelProveedor("simulado"), proveedoresSimulados());
        textosRenovados = renovacion.renovar(List.of(pedidoDeRenovacion(), pedidoDeRenovacion()));
    }

    private void whenSeConectaLaRenovacionConProveedorInexistente() {
        error = catchThrowable(() -> config.renovacionManualesIaPort(config.promptRenovacionManuales(),
                JsonMapper.builder().build(), sami(), propiedadesDelProveedor("inexistente"),
                new ProveedoresIa(List.of())));
    }

    // --- assert ---
    private void thenElPedidoDeObjetivosTieneLasInstrucciones() {
        assertThat(promptObjetivos.peticion(List.of(candidato())).instrucciones()).contains("Sos Sami");
    }

    private void thenElRedactorEsElAdaptadorYNoSabeEscribir() {
        assertThat(redaccion).isInstanceOf(RedaccionObjetivosIaAdapter.class);
        assertThat(catchThrowable(() -> redaccion.redactar(List.of(candidato()))))
                .isInstanceOf(IllegalStateException.class);
    }

    private void thenNoLevanta() {
        assertThat(error).isInstanceOf(IllegalStateException.class);
    }

    private void thenElPedidoDeConversacionTieneLasInstrucciones() {
        assertThat(promptConversacion.peticion(consulta()).instrucciones()).contains("Sos Sami");
    }

    private void thenLaConversacionProponeUnaMetricaYUnaAccionSinCosto() {
        assertThat(conversacion).isInstanceOf(ConversacionObjetivosIaAdapter.class);
        assertThat(PropuestaObjetivo.de(propuesta, consulta()).minis())
                .extracting(MiniObjetivoPropuesto::modo)
                .containsExactly(ModoMiniObjetivo.MEDIDO, ModoMiniObjetivo.MANUAL);
        assertThat(propuesta.consumo().tokensEntrada()).isZero();
    }

    private void thenElPedidoDeRenovacionTieneLasInstrucciones() {
        assertThat(promptRenovacion.peticion(List.of(pedidoDeRenovacion())).instrucciones()).contains("Sos Sami");
    }

    private void thenLaRenovacionTieneUnTextoPorCadaUnaSinCosto() {
        assertThat(renovacion).isInstanceOf(RenovacionManualesIaAdapter.class);
        assertThat(textosRenovados.textos()).hasSize(2);
        assertThat(textosRenovados.consumo().tokensEntrada()).isZero();
    }

    // --- helpers ---
    private static SamiProperties sami() {
        return new SamiProperties(0.7, 3, 20, 60,
                new SamiProperties.Ia(null, null, null, null, null, Duration.ofSeconds(30), 700, 0.6, 8000));
    }

    private static IaProperties propiedadesDelProveedor(String proveedor) {
        return new IaProperties(proveedor, null, Duration.ofSeconds(60), 10, 0.8, Map.of());
    }

    private static ProveedoresIa proveedoresSimulados() {
        return new ProveedoresIa(List.of(new ProveedorIaSimulado(
                new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales)));
    }

    private static PedidoRenovacion pedidoDeRenovacion() {
        return new PedidoRenovacion(AccionRenovacion.SE_DIVIDE, "Un cartel", "Que te vean", HizoManual.EN_PARTE,
                QuePaso.NO_TUVE_TIEMPO, null);
    }
}
