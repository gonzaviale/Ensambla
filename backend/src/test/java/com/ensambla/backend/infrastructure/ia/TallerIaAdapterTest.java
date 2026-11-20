package com.ensambla.backend.infrastructure.ia;

import com.ensambla.backend.domain.model.sami.ConsumoIaSami;
import com.ensambla.backend.domain.model.sami.taller.ConsultaTaller;
import com.ensambla.backend.domain.model.sami.taller.EstadoTaller;
import com.ensambla.backend.domain.model.sami.taller.LlamadaHerramienta;
import com.ensambla.backend.domain.model.sami.taller.ResultadoTallerIa;
import com.ensambla.backend.support.SamiPruebas;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class TallerIaAdapterTest {

    private final PromptTaller prompt = new PromptTaller(new ClassPathResource("ia/prompt-taller.md"),
            SamiPruebas.catalogoReal(), SamiPruebas.kitsReales());
    private final List<PeticionConHerramientas> pedidos = new ArrayList<>();

    private TallerIaAdapter adapter;
    private ResultadoTallerIa resultado;
    private Throwable rechazo;

    @Test
    @DisplayName("Hace una sola llamada con el pedido armado y devuelve texto, llamadas y consumo")
    void haceUnaSolaLlamadaConElPedidoArmadoYDevuelveTextoLlamadasYConsumo() {
        dadoUnClienteQueSumaGastos();

        cuandoSeConversaSobreGastos();

        entoncesSeHaceUnaLlamadaYSeDevuelveTextoLlamadasYConsumo();
    }

    @Test
    @DisplayName("Si el cliente falla deja pasar el error para que el servicio lo traduzca")
    void siElClienteFallaDejaPasarElErrorParaQueElServicioLoTraduzca() {
        dadoUnClienteQueFallaPorTimeout();

        cuandoSeIntentaConversar();

        entoncesSeDejaPasarElError();
    }

    // --- dado ---

    private void dadoUnClienteQueSumaGastos() {
        adapter = new TallerIaAdapter(peticion -> {
            pedidos.add(peticion);
            return new RespuestaConHerramientas("Listo", List.of(
                    new LlamadaHerramienta("agregar_pieza", Map.of("tipo", "expenses"))), "bedrock", "haiku", 800, 90);
        }, prompt);
    }

    private void dadoUnClienteQueFallaPorTimeout() {
        adapter = new TallerIaAdapter(peticion -> {
            throw new IllegalStateException("timeout");
        }, prompt);
    }

    // --- cuando ---

    private void cuandoSeConversaSobreGastos() {
        resultado = adapter.conversar(
                new ConsultaTaller("gastos", new EstadoTaller("S", "mosaic", List.of()), List.of()));
    }

    private void cuandoSeIntentaConversar() {
        rechazo = catchThrowable(() -> adapter.conversar(
                new ConsultaTaller("x", new EstadoTaller("S", "mosaic", List.of()), List.of())));
    }

    // --- entonces ---

    private void entoncesSeHaceUnaLlamadaYSeDevuelveTextoLlamadasYConsumo() {
        assertThat(pedidos).hasSize(1);
        assertThat(pedidos.getFirst().contexto()).endsWith("Mensaje de la persona: gastos");
        assertThat(resultado.texto()).isEqualTo("Listo");
        assertThat(resultado.llamadas()).hasSize(1);
        assertThat(resultado.consumo()).isEqualTo(new ConsumoIaSami("bedrock", "haiku", 800, 90));
    }

    private void entoncesSeDejaPasarElError() {
        assertThat(rechazo).isInstanceOf(IllegalStateException.class);
    }
}
