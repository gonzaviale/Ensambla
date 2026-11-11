package com.ensambla.backend.domain.model.pedido;

import com.ensambla.backend.domain.model.negocio.NegocioId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextosPedidoTest {

    private static final Comercio PEPE = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000001"),
            "Almacén Don Pepe");
    private static final Comercio ROSA = new Comercio(NegocioId.de("a0000000-0000-4000-8000-000000000002"),
            "Almacén Doña Rosa");

    @Test
    @DisplayName("La oferta de seguir nombra al último comercio y explica cómo cambiarlo")
    void bienvenidaConUltimoShouldNameTheBusiness() {
        assertThat(TextosPedido.bienvenidaConUltimo("Almacén Don Pepe"))
                .contains("*Almacén Don Pepe*")
                .contains("*SÍ*");
    }

    @Test
    @DisplayName("El aviso de que no encontró el comercio repite lo que se escribió sin los espacios de los bordes")
    void comercioNoEncontradoShouldEchoTheText() {
        assertThat(TextosPedido.comercioNoEncontrado("  panadería luna ")).contains("«panadería luna»");
    }

    @Test
    @DisplayName("La lista de comercios parecidos los numera desde 1 y avisa cómo descartarla")
    void variosComerciosShouldNumberTheOptions() {
        assertThat(TextosPedido.variosComercios(List.of(PEPE, ROSA))).isEqualTo("""
                Encontré varios comercios parecidos. Respondé con el número:
                1) Almacén Don Pepe
                2) Almacén Doña Rosa
                O respondé NO para escribir otro nombre.""");
    }

    @Test
    @DisplayName("Una opción inválida dice entre qué números elegir")
    void opcionInvalidaShouldTellTheRange() {
        assertThat(TextosPedido.opcionInvalida(3)).contains("entre 1 y 3");
    }

    @Test
    @DisplayName("Al elegir el comercio le dice quién lo atiende y cómo cambiar")
    void comercioElegidoShouldNameTheBusiness() {
        assertThat(TextosPedido.comercioElegido("Kiosco Sol")).contains("*Kiosco Sol*").contains("*CAMBIAR*");
    }
}
