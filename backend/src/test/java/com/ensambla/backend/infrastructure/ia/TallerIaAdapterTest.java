package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TallerIaAdapterTest {

    private final PromptTaller prompt = new PromptTaller(new ClassPathResource("ia/prompt-taller.md"));
    private final List<PeticionConHerramientas> pedidos = new ArrayList<>();

    @Test
    @DisplayName("Hace una sola llamada con el pedido armado y devuelve texto, llamadas y consumo")
    void conversarShouldCallOnceAndReturnTextCallsAndConsumption() {
        TallerIaAdapter adapter = new TallerIaAdapter(peticion -> {
            pedidos.add(peticion);
            return new RespuestaConHerramientas("Listo", List.of(
                    new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), "bedrock", "haiku", 800, 90);
        }, prompt);

        ResultadoTallerIa result = adapter.conversar(
                new ConsultaTaller("gastos", new EstadoTaller("S", "mosaic", List.of()), List.of()));

        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.getFirst().contexto()).endsWith("Mensaje de la persona: gastos");
        assertThat(result.texto()).isEqualTo("Listo");
        assertThat(result.llamadas()).hasSize(1);
        assertThat(result.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 800, 90));
    }

    @Test
    @DisplayName("Si el cliente falla deja pasar el error para que el servicio lo traduzca")
    void conversarShouldPropagateClientFailure() {
        TallerIaAdapter adapter = new TallerIaAdapter(peticion -> {
            throw new IllegalStateException("timeout");
        }, prompt);

        assertThatThrownBy(() -> adapter.conversar(
                new ConsultaTaller("x", new EstadoTaller("S", "mosaic", List.of()), List.of())))
                .isInstanceOf(IllegalStateException.class);
    }
}
