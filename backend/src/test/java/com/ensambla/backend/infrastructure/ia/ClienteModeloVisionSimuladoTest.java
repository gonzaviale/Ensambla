package com.ensambla.backend.infrastructure.ia;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClienteModeloVisionSimuladoTest {

    private static final String LECTURA = "{\"tipoRegistro\":\"VENTAS\"}";

    @Test
    @DisplayName("Devuelve siempre la lectura de ejemplo sin consumir tokens")
    void completarShouldReturnLecturaDeEjemploWithoutTokens() {
        ClienteModeloVisionSimulado cliente = new ClienteModeloVisionSimulado(
                new ByteArrayResource(LECTURA.getBytes(StandardCharsets.UTF_8)));

        RespuestaModeloVision respuesta = cliente.completar(new PeticionModeloVision("i", "c", List.of(), "{}"));

        thenIsExampleWithoutTokens(respuesta);
    }

    // --- assert ---
    private void thenIsExampleWithoutTokens(RespuestaModeloVision respuesta) {
        assertThat(respuesta.json()).isEqualTo(LECTURA);
        assertThat(respuesta.proveedor()).isEqualTo(ClienteModeloVisionSimulado.PROVEEDOR);
        assertThat(respuesta.modelo()).isEqualTo(ClienteModeloVisionSimulado.MODELO);
        assertThat(respuesta.tokensEntrada()).isZero();
        assertThat(respuesta.tokensSalida()).isZero();
    }
}
