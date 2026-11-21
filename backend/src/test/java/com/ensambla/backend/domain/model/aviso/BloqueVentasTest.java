package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueVentasTest {

    private static final String PRODUCTO_ID = UUID.fromString("d0000000-0000-4000-8000-000000000001").toString();

    private Registro venta;
    private BloqueVentas ventas;
    private Object valor;

    @Test
    @DisplayName("Devuelve el valor que la venta cargó para la clave")
    void devuelveElValorQueLaVentaCargoParaLaClave() {
        dadoUnBloqueConUnaVentaDeDosUnidades();

        cuandoSeConsultaElValorDe("quantity");

        entoncesElValorEs(2);
    }

    @Test
    @DisplayName("Devuelve null cuando la venta no cargó el dato")
    void devuelveNullCuandoLaVentaNoCargoElDato() {
        dadoUnBloqueConUnaVentaDeDosUnidades();

        cuandoSeConsultaElValorDe("customer");

        entoncesNoHayValor();
    }

    @Test
    @DisplayName("Devuelve null cuando la tabla no tiene un campo con esa clave")
    void devuelveNullCuandoLaTablaNoTieneUnCampoConEsaClave() {
        dadoUnBloqueConUnaVentaDeDosUnidades();

        cuandoSeConsultaElValorDe("minimum");

        entoncesNoHayValor();
    }

    // --- dado ---

    private void dadoUnBloqueConUnaVentaDeDosUnidades() {
        venta = AvisoTestData.venta(PRODUCTO_ID, 2, 100, "2026-10-18", null);
        ventas = AvisoTestData.ventas(venta);
    }

    // --- cuando ---

    private void cuandoSeConsultaElValorDe(String clave) {
        valor = ventas.valorDe(venta, clave);
    }

    // --- entonces ---

    private void entoncesElValorEs(Object esperado) {
        assertThat(valor).isEqualTo(esperado);
    }

    private void entoncesNoHayValor() {
        assertThat(valor).isNull();
    }
}
