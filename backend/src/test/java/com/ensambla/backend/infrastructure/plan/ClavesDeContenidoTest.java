package com.ensambla.backend.infrastructure.plan;

import com.ensambla.backend.domain.model.plan.TipoContenidoPlan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ClavesDeContenidoTest {

    @Test
    @DisplayName("Los avisos se guardan con el prefijo del servidor")
    void guardadaShouldPrefixAvisos() {
        assertThat(ClavesDeContenido.guardada(TipoContenidoPlan.AVISOS, "a1")).isEqualTo("servidor:a1");
    }

    @Test
    @DisplayName("Los contenidos de zona se guardan tal cual")
    void guardadaShouldKeepZonaKeys() {
        assertThat(ClavesDeContenido.guardada(TipoContenidoPlan.ZONA, "yerba")).isEqualTo("yerba");
    }

    @Test
    @DisplayName("Al leer un aviso se le quita el prefijo del servidor")
    void delDominioShouldRemovePrefix() {
        assertThat(ClavesDeContenido.delDominio(TipoContenidoPlan.AVISOS, "servidor:a1")).isEqualTo("a1");
    }

    @Test
    @DisplayName("Una clave guardada sin prefijo se lee igual")
    void delDominioShouldKeepKeysWithoutPrefix() {
        assertThat(ClavesDeContenido.delDominio(TipoContenidoPlan.AVISOS, "demo:1")).isEqualTo("demo:1");
        assertThat(ClavesDeContenido.delDominio(TipoContenidoPlan.ZONA, "yerba")).isEqualTo("yerba");
    }

    @Test
    @DisplayName("Cada clave del pedido se asocia a la clave del dominio en el mismo orden")
    void origenesDelPedidoShouldMapDomainKeysToRequestedOnes() {
        Map<String, String> origenes = ClavesDeContenido.origenesDelPedido(
                TipoContenidoPlan.AVISOS, List.of("servidor:a1", "demo:1"));

        assertThat(origenes).containsExactly(Map.entry("a1", "servidor:a1"), Map.entry("demo:1", "demo:1"));
    }
}
