package com.ensambla.backend.domain.model.promocion;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static com.ensambla.backend.support.PromocionDePrueba.promocionDeYerba;
import static org.assertj.core.api.Assertions.assertThat;

/** Lo que cumple todo canal para compartir una promoción; cada canal lo hereda y suma lo propio. */
abstract class CanalPromocionContratoTest {

    protected static final String NEGOCIO = "Almacén de Ana";

    private static final String NOMBRE_LARGO = "Producto con un nombre larguísimo número ".repeat(5);

    protected abstract CanalPromocion crear();

    @Test
    @DisplayName("Tiene una clave y un nombre para mostrar")
    void claveAndNombreShouldNotBeBlank() {
        CanalPromocion canal = crear();

        assertThat(canal.clave()).isNotBlank();
        assertThat(canal.nombre()).isNotBlank();
    }

    @Test
    @DisplayName("El texto nombra al negocio, a cada producto y a su precio con puntos de miles")
    void redactarShouldMentionTheBusinessAndEveryProduct() {
        String result = crear().redactar(NEGOCIO, promocionDeYerba());

        assertThat(result).contains(NEGOCIO, "Yerba Playadito 1 kg", "$ 3.600");
    }

    @Test
    @DisplayName("Escribe un renglón por producto")
    void redactarShouldWriteOneLinePerProduct() {
        Promocion promocion = promocionConProductos(3, "Producto ");

        String result = crear().redactar(NEGOCIO, promocion);

        assertThat(result).contains("• Producto 1: $ 100", "• Producto 2: $ 100", "• Producto 3: $ 100");
    }

    @Test
    @DisplayName("Nunca pasa del largo que admite el canal y marca el corte con puntos suspensivos")
    void redactarShouldNeverExceedTheMaximumLength() {
        CanalPromocion canal = crear();

        String result = canal.redactar(NEGOCIO, promocionConProductos(12, NOMBRE_LARGO));

        assertThat(result.length()).isLessThanOrEqualTo(canal.largoMaximo());
        assertThat(result).endsWith("…");
    }

    // --- arrange ---
    protected static Promocion promocionConProductos(int cantidad, String prefijoDelNombre) {
        List<ProductoPromocionado> productos = IntStream.rangeClosed(1, cantidad)
                .mapToObj(numero -> new ProductoPromocionado(prefijoDelNombre + numero, new BigDecimal("100")))
                .toList();
        return new Promocion("prueba", "Título de la promoción", "Gancho de la promoción", productos);
    }
}
