package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.cartel.DatosDePromocion;
import com.ensambla.backend.domain.model.cartel.PedidoDeTextosDeCartel;
import com.ensambla.backend.domain.model.cartel.RedaccionDeCartelIa;
import com.ensambla.backend.domain.model.cartel.TipoDeCartel;
import com.ensambla.backend.domain.model.marca.TonoDeMarca;
import com.ensambla.backend.domain.model.negocio.RubroNegocio;
import com.ensambla.backend.domain.port.RedaccionDeCartelIaPort;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CartelesIaTest {

    private static final PedidoDeTextosDeCartel PEDIDO = new PedidoDeTextosDeCartel("Don Rufino", RubroNegocio.ALMACEN,
            TonoDeMarca.CERCANO, new DatosDePromocion(List.of(TipoDeCartel.PROMO_PRECIO, TipoDeCartel.NOVEDAD),
            List.of("Aceite de girasol 900 ml", "Yerba"), List.of(new BigDecimal("2500"), new BigDecimal("20"))));

    private final ClienteModeloTexto modelo = mock(ClienteModeloTexto.class);
    private final CartelesIaConfig config = new CartelesIaConfig();

    private RedaccionDeCartelIa redaccion;

    @Test
    @DisplayName("El pedido a la IA lleva el negocio, el tono, las plantillas, los productos y los números permitidos")
    void promptShouldDescribeTheRequestWithoutPersonalData() {
        PeticionModeloTexto peticion = prompt().peticion(PEDIDO);

        assertThat(peticion.instrucciones()).contains("redactor de carteles");
        assertThat(peticion.esquemaJson()).contains("textoPosteo");
        assertThat(peticion.contexto()).contains("Negocio: «Don Rufino» (rubro: Almacén)")
                .contains("Tono: cercano")
                .contains("Plantillas posibles: PROMO_PRECIO, NOVEDAD")
                .contains("Productos: «Aceite de girasol 900 ml», «Yerba»")
                .contains("Números permitidos: 2500, 20");
    }

    @Test
    @DisplayName("El redactor lee la respuesta válida del modelo y devuelve lo que gastó")
    void adapterShouldReadAValidResponse() {
        givenQueElModeloResponde("{\"plantilla\":\"NOVEDAD\",\"titular\":\"Llegó la yerba\",\"bajada\":\"Ya está\","
                + "\"textoPosteo\":\"Llegó la yerba\",\"extra\":1}");

        whenSeRedacta();

        assertThat(redaccion.textos().plantilla()).isEqualTo(TipoDeCartel.NOVEDAD);
        assertThat(redaccion.consumo().tokensEntrada()).isEqualTo(30);
        assertThat(redaccion.consumo().proveedor()).isEqualTo("prueba");
    }

    @Test
    @DisplayName("El redactor rechaza un JSON ilegible, una plantilla desconocida o un texto vacío")
    void adapterShouldRejectBadResponses() {
        givenQueElModeloResponde("no es json");
        assertThatThrownBy(this::whenSeRedacta).isInstanceOf(RuntimeException.class);

        givenQueElModeloResponde("{\"plantilla\":\"INVENTADA\",\"titular\":\"t\",\"bajada\":\"b\",\"textoPosteo\":\"p\"}");
        assertThatThrownBy(this::whenSeRedacta).isInstanceOf(IllegalArgumentException.class);

        givenQueElModeloResponde("{\"plantilla\":\"NOVEDAD\",\"titular\":\"\",\"bajada\":\"b\",\"textoPosteo\":\"p\"}");
        assertThatThrownBy(this::whenSeRedacta).isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("El proveedor simulado redacta con los textos de reserva y no gasta nada")
    void simuladaShouldWriteTheReserveTexts() {
        RedaccionDeCartelIaPort simulada = config.redaccionDeCartelIaSimulada();

        redaccion = simulada.redactar(PEDIDO);

        assertThat(redaccion.textos().titular()).startsWith("¡Oferta en Aceite");
        assertThat(redaccion.consumo().tokensSalida()).isZero();
    }

    @Test
    @DisplayName("Con el proveedor real conecta el redactor con el modelo de texto de Sami")
    void configShouldWireTheRealProvider() {
        RedaccionDeCartelIaPort real = config.redaccionDeCartelIaReal(
                JsonMapper.builder().build(),
                new SamiProperties(0.7, 3, 20, 60,
                        new SamiProperties.Ia(null, null, null, null, null, Duration.ofSeconds(30), 700, 0.6, 8000)),
                new IaProperties("simulado", null, Duration.ofSeconds(60), 10, 0.8, Map.of()),
                new ProveedoresIa(List.of(new ProveedorIaSimulado(
                        new ClassPathResource("ia/lectura-simulada.json"), SamiPruebas::kitsReales))));

        assertThat(real).isInstanceOf(RedaccionDeCartelIaAdapter.class);
    }

    @Test
    @DisplayName("Por defecto el proveedor de carteles es el simulado")
    void propertiesShouldDefaultToSimulated() {
        assertThat(new CartelesIaProperties("simulado").proveedor()).isEqualTo("simulado");
    }

    private PromptCarteles prompt() {
        return new PromptCarteles(
                new ClassPathResource("ia/prompt-carteles.md"), new ClassPathResource("ia/esquema-carteles.json"));
    }

    private void givenQueElModeloResponde(String json) {
        when(modelo.completar(any())).thenReturn(new RespuestaModeloTexto(json, "prueba", "modelo", 30, 12));
    }

    private void whenSeRedacta() {
        redaccion = new RedaccionDeCartelIaAdapter(modelo, prompt(), JsonMapper.builder().build()).redactar(PEDIDO);
    }
}
