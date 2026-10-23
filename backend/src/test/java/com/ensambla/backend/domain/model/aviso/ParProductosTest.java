package com.ensambla.backend.domain.model.aviso;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ParProductosTest {

    private static final UUID PRIMERO = UUID.fromString("d0000000-0000-4000-8000-000000000001");
    private static final UUID SEGUNDO = UUID.fromString("d0000000-0000-4000-8000-000000000002");

    @Test
    @DisplayName("Deja el par como llegó cuando ya está ordenado")
    void deShouldKeepOrderWhenAlreadySorted() {
        ParProductos par = ParProductos.de(PRIMERO, SEGUNDO, 3);

        assertThat(par.productoA()).isEqualTo(PRIMERO);
        assertThat(par.productoB()).isEqualTo(SEGUNDO);
        assertThat(par.veces()).isEqualTo(3);
    }

    @Test
    @DisplayName("Ordena el par para que sea el mismo sin importar cuál se anotó primero")
    void deShouldSortTheProducts() {
        ParProductos par = ParProductos.de(SEGUNDO, PRIMERO, 3);

        assertThat(par).isEqualTo(ParProductos.de(PRIMERO, SEGUNDO, 3));
        assertThat(par.productoA()).isEqualTo(PRIMERO);
    }
}
