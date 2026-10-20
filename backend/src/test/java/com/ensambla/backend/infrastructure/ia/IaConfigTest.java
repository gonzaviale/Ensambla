package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.lectura.LecturaCuaderno;
import com.ensambla.backend.domain.model.lectura.PaginaCuaderno;
import com.ensambla.backend.domain.model.sami.taller.KitsRubro;
import com.ensambla.backend.domain.port.LectorCuadernoPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IaConfigTest {

    private static final IaProperties SIMULADO =
            new IaProperties("simulado", null, Duration.ofSeconds(60), 10, 0.8, null);
    private static final IaProperties.Proveedor BEDROCK = new IaProperties.Proveedor(
            null, "us-east-1", null, "rapido", "avanzado", "vision-bedrock");

    private IaConfig config;

    @BeforeEach
    void setUp() {
        config = new IaConfig();
    }

    @Test
    @DisplayName("Con el proveedor simulado, el lector de cuadernos devuelve la lectura de ejemplo validada")
    void lectorCuadernoShouldReadExampleWithSimulatedProvider() {
        LectorCuadernoPort lector = config.lectorCuaderno(
                config.clienteModeloVision(proveedores(), SIMULADO),
                config.promptLecturaCuaderno(), JsonMapper.builder().build(), SIMULADO);

        LecturaCuaderno lectura = lector.leer(List.of(new PaginaCuaderno(1, new byte[]{1}, "image/jpeg")), List.of(), null);

        thenExampleLecturaIsValid(lectura);
    }

    @Test
    @DisplayName("La lectura pide la puerta de visión al proveedor global, con su modelo de visión")
    void clienteModeloVisionShouldAskTheGlobalProviderForVision() {
        ProveedorIa bedrock = givenProveedor("bedrock");
        IaProperties properties = new IaProperties("bedrock", null, Duration.ofSeconds(60), 10, 0.8,
                Map.of("bedrock", BEDROCK));

        config.clienteModeloVision(new ProveedoresIa(List.of(bedrock)), properties);

        verify(bedrock).vision(new AjustesIa("bedrock", null, "us-east-1", null, "rapido", "avanzado",
                "vision-bedrock", Duration.ofSeconds(60)));
    }

    @Test
    @DisplayName("Un proveedor que no existe no deja levantar la lectura")
    void clienteModeloVisionShouldFailWithUnknownProvider() {
        IaProperties properties = new IaProperties("openai", null, Duration.ofSeconds(60), 10, 0.8, null);

        assertThatThrownBy(() -> config.clienteModeloVision(proveedores(), properties))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("openai")
                .hasMessageContaining("anthropic, bedrock, simulado");
    }

    @Test
    @DisplayName("Registra los tres proveedores con sus nombres")
    void proveedoresShouldBeNamedAfterTheirSetting() {
        assertThat(config.proveedorIaSimulado(kits()).nombre()).isEqualTo("simulado");
        assertThat(config.proveedorIaSimulado(kits()).conHerramientas(null, 900, List.of()))
                .isInstanceOf(ClienteModeloConHerramientasSimulado.class);
        assertThat(config.proveedorIaAnthropic().nombre()).isEqualTo("anthropic");
        assertThat(config.proveedorIaBedrock().nombre()).isEqualTo("bedrock");
    }

    // --- arrange ---
    private ProveedorIa givenProveedor(String nombre) {
        ProveedorIa proveedor = mock(ProveedorIa.class);
        when(proveedor.nombre()).thenReturn(nombre);
        return proveedor;
    }

    // --- helpers ---
    private ProveedoresIa proveedores() {
        return config.proveedoresIa(List.of(config.proveedorIaSimulado(kits()), config.proveedorIaAnthropic(),
                config.proveedorIaBedrock()));
    }

    /** Los kits como los ofrece Spring: el bean de {@link SamiConfig}. */
    private ObjectProvider<KitsRubro> kits() {
        StaticListableBeanFactory beans = new StaticListableBeanFactory();
        beans.addBean("kitsRubro", new SamiConfig().kitsRubro(JsonMapper.builder().build()));
        return beans.getBeanProvider(KitsRubro.class);
    }

    // --- assert ---
    private void thenExampleLecturaIsValid(LecturaCuaderno lectura) {
        assertThat(lectura.bloque().nombre()).isEqualTo("Ventas");
        assertThat(lectura.bloque().campos()).hasSize(6);
        assertThat(lectura.filas()).hasSize(3);
        assertThat(lectura.filas()).filteredOn(fila -> fila.tieneDudas(SIMULADO.umbralConfianza())).hasSize(2);
        assertThat(lectura.advertencias()).hasSize(1);
        assertThat(lectura.consumo().proveedor()).isEqualTo(ClienteModeloVisionSimulado.PROVEEDOR);
    }
}
