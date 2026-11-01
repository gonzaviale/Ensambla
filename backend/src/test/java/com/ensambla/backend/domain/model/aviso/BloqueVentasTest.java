package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueVentasTest {

    private static final String PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001").toString();

    @Test
    @DisplayName("Devuelve el valor que la venta cargó para la clave")
    void valorDeShouldReturnValueOfRegistro() {
        Registro venta = AvisoTestData.venta(PRODUCTO_ID, 2, 100, "2026-10-18", null);
        BloqueVentas ventas = AvisoTestData.ventas(venta);

        assertThat(ventas.valorDe(venta, "quantity")).isEqualTo(2);
    }

    @Test
    @DisplayName("Devuelve null cuando la venta no cargó el dato")
    void valorDeShouldReturnNullWhenRegistroHasNoValue() {
        Registro venta = AvisoTestData.venta(PRODUCTO_ID, 2, 100, "2026-10-18", null);
        BloqueVentas ventas = AvisoTestData.ventas(venta);

        assertThat(ventas.valorDe(venta, "customer")).isNull();
    }

    @Test
    @DisplayName("Devuelve null cuando la tabla no tiene un campo con esa clave")
    void valorDeShouldReturnNullWhenTablaHasNoCampoWithClave() {
        Registro venta = AvisoTestData.venta(PRODUCTO_ID, 2, 100, "2026-10-18", null);
        BloqueVentas ventas = AvisoTestData.ventas(venta);

        assertThat(ventas.valorDe(venta, "minimum")).isNull();
    }
}
