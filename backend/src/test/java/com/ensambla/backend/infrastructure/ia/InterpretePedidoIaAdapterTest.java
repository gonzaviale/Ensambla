package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.pedido.ConsultaPedido;
import com.ensambla.backend.domain.model.pedido.LineaPedida;
import com.ensambla.backend.domain.model.pedido.PedidoInterpretado;
import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterpretePedidoIaAdapterTest {

    private final PromptWhatsappPedido prompt = new PromptWhatsappPedido(
            new ClassPathResource("ia/prompt-whatsapp-pedido.md"),
            new ClassPathResource("ia/esquema-whatsapp-pedido.json"));
    private final List<PeticionModeloTexto> peticiones = new ArrayList<>();
    private final ConsultaPedido consulta = new ConsultaPedido("dame 1 kilo de yerba y aceite",
            List.of("Yerba Playadito 1 kg", "Aceite Cocinero 900 ml"));

    @Test
    @DisplayName("El pedido a la IA lleva los productos del comercio y el mensaje, con las instrucciones y el esquema")
    void pedidoShouldCarryTheProductsAndTheMessage() {
        PeticionModeloTexto peticion = prompt.pedido(consulta);

        assertThat(peticion.contexto()).contains("Productos del comercio:", "- Yerba Playadito 1 kg",
                "- Aceite Cocinero 900 ml", "dame 1 kilo de yerba y aceite");
        assertThat(peticion.instrucciones()).contains("Sos Sami");
        assertThat(peticion.esquemaJson()).contains("\"lineas\"");
    }

    @Test
    @DisplayName("Sin productos en el comercio el pedido no trae la lista")
    void pedidoShouldOmitTheProductListWhenThereAreNone() {
        assertThat(prompt.pedido(new ConsultaPedido("hola", List.of())).contexto())
                .doesNotContain("Productos del comercio").contains("hola");
    }

    @Test
    @DisplayName("Traduce la respuesta a líneas con su cantidad y devuelve el consumo")
    void interpretarShouldTranslateTheLines() {
        PedidoInterpretado pedido = adapter(respuesta("""
                {"lineas":[{"producto":" Yerba Playadito 1 kg ","cantidad":1},{"producto":"aceite","cantidad":null}]}"""))
                .interpretar(consulta);

        assertThat(pedido.lineas()).containsExactly(new LineaPedida("Yerba Playadito 1 kg", BigDecimal.ONE),
                new LineaPedida("aceite", null));
        assertThat(pedido.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 100, 20));
        assertThat(peticiones).hasSize(1);
    }

    @Test
    @DisplayName("Descarta las líneas sin producto")
    void interpretarShouldDropLinesWithoutProduct() {
        PedidoInterpretado pedido = adapter(respuesta("""
                {"lineas":[{"producto":"","cantidad":1},{"producto":"  ","cantidad":2},{"cantidad":3},null,
                {"producto":"fideos","cantidad":2}]}""")).interpretar(consulta);

        assertThat(pedido.lineas()).containsExactly(new LineaPedida("fideos", BigDecimal.valueOf(2)));
    }

    @Test
    @DisplayName("Una respuesta vacía, sin líneas o ilegible vuelve sin líneas pero con su consumo")
    void interpretarShouldReturnEmptyWithConsumption() {
        for (String json : List.of("{\"lineas\":[]}", "{}", "null", "esto no es json",
                "{\"lineas\":[{\"producto\":\"x\",\"cantidad\":\"muchas\"}]}")) {
            PedidoInterpretado pedido = adapter(respuesta(json)).interpretar(consulta);

            assertThat(pedido.lineas()).isEmpty();
            assertThat(pedido.consumo()).isNotNull();
        }
    }

    @Test
    @DisplayName("Si el modelo no responde propaga el error del cliente")
    void interpretarShouldPropagateClientFailure() {
        InterpretePedidoIaAdapter adapter = new InterpretePedidoIaAdapter(peticion -> {
            throw new IllegalStateException("sin conexión");
        }, prompt, JsonMapper.builder().build());

        assertThatThrownBy(() -> adapter.interpretar(consulta)).hasMessage("sin conexión");
    }

    private InterpretePedidoIaAdapter adapter(RespuestaModeloTexto respuesta) {
        return new InterpretePedidoIaAdapter(peticion -> {
            peticiones.add(peticion);
            return respuesta;
        }, prompt, JsonMapper.builder().build());
    }

    private static RespuestaModeloTexto respuesta(String json) {
        return new RespuestaModeloTexto(json, "bedrock", "haiku", 100, 20);
    }
}
