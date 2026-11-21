package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IaPropertiesTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);
    private static final IaProperties.Proveedor ANTHROPIC = new IaProperties.Proveedor(
            "clave", null, null, "claude-haiku-4-5", "claude-sonnet-4-6", "claude-haiku-4-5");
    private static final IaProperties.Proveedor BEDROCK = new IaProperties.Proveedor(
            null, "us-west-2", "perfil", "haiku-bedrock", "sonnet-bedrock", "vision-bedrock");
    private static final Map<String, IaProperties.Proveedor> PROVEEDORES =
            Map.of("anthropic", ANTHROPIC, "bedrock", BEDROCK);

    private IaProperties properties;
    private AjustesIa ajustes;

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("Sin proveedor, el global es el simulado")
    void sinProveedorElGlobalEsElSimulado(String proveedor) {
        dadoUnaConfiguracionConProveedor(proveedor, null);

        entoncesElProveedorEs("simulado");
    }

    @Test
    @DisplayName("El nombre del proveedor no depende de mayúsculas ni espacios")
    void elNombreDelProveedorNoDependeDeMayusculasNiEspacios() {
        dadoUnaConfiguracionConProveedor(" Bedrock ", null);

        entoncesElProveedorEs("bedrock");
    }

    @Test
    @DisplayName("Los ajustes globales salen de las credenciales y los modelos del proveedor elegido")
    void losAjustesGlobalesSalenDeLasCredencialesYLosModelosDelProveedorElegido() {
        dadoUnaConfiguracionConProveedor("bedrock", null);

        cuandoSePidenLosAjustes();

        entoncesLosAjustesSon(new AjustesIa("bedrock", null, "us-west-2", "perfil", "haiku-bedrock",
                "sonnet-bedrock", "vision-bedrock", TIMEOUT));
    }

    @Test
    @DisplayName("ENSAMBLA_IA_MODELO pisa el modelo de visión del proveedor")
    void ensamblaIaModeloPisaElModeloDeVisionDelProveedor() {
        dadoUnaConfiguracionConProveedor("anthropic", "claude-opus");

        cuandoSePidenLosAjustes();

        entoncesElModeloDeVisionEsClaudeOpusYLaClaveSigueSiendoLaDelProveedor();
    }

    @Test
    @DisplayName("El simulado no tiene credenciales ni modelos")
    void elSimuladoNoTieneCredencialesNiModelos() {
        dadoUnaConfiguracionConProveedor("simulado", null);

        cuandoSePidenLosAjustes();

        entoncesLosAjustesSon(new AjustesIa("simulado", null, null, null, null, null, null, TIMEOUT));
    }

    @Test
    @DisplayName("Sin proveedores configurados, cualquiera queda sin datos")
    void sinProveedoresConfiguradosCualquieraQuedaSinDatos() {
        dadoUnaConfiguracionSinProveedores();

        entoncesCualquierProveedorQuedaSinDatosYSinModeloDeVision();
    }

    @Test
    @DisplayName("Un modelo de visión en blanco cuenta como que no hay ninguno")
    void unModeloDeVisionEnBlancoCuentaComoQueNoHayNinguno() {
        dadoUnaConfiguracionConModelosDeVisionEnBlanco();

        entoncesNoHayModeloDeVision();
    }

    // --- dado ---

    private void dadoUnaConfiguracionConProveedor(String proveedor, String modelo) {
        properties = new IaProperties(proveedor, modelo, TIMEOUT, 10, 0.8, PROVEEDORES);
    }

    private void dadoUnaConfiguracionSinProveedores() {
        properties = new IaProperties("bedrock", null, TIMEOUT, 10, 0.8, null);
    }

    private void dadoUnaConfiguracionConModelosDeVisionEnBlanco() {
        properties = new IaProperties("bedrock", " ", TIMEOUT, 10, 0.8,
                Map.of("bedrock", new IaProperties.Proveedor(null, "us-east-1", null, "rapido", null, "")));
    }

    // --- cuando ---

    private void cuandoSePidenLosAjustes() {
        ajustes = properties.ajustes();
    }

    // --- entonces ---

    private void entoncesElProveedorEs(String esperado) {
        assertThat(properties.proveedor()).isEqualTo(esperado);
    }

    private void entoncesLosAjustesSon(AjustesIa esperados) {
        assertThat(ajustes).isEqualTo(esperados);
    }

    private void entoncesElModeloDeVisionEsClaudeOpusYLaClaveSigueSiendoLaDelProveedor() {
        assertThat(ajustes.modeloVision()).isEqualTo("claude-opus");
        assertThat(ajustes.apiKey()).isEqualTo("clave");
        assertThat(properties.modeloVision()).isEqualTo("claude-opus");
    }

    private void entoncesCualquierProveedorQuedaSinDatosYSinModeloDeVision() {
        assertThat(properties.de("bedrock"))
                .isEqualTo(new IaProperties.Proveedor(null, null, null, null, null, null));
        assertThat(properties.modeloVision()).isNull();
    }

    private void entoncesNoHayModeloDeVision() {
        assertThat(properties.modeloVision()).isNull();
    }
}
