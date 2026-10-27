package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueInventarioTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    @Test
    @DisplayName("Devuelve el valor que el registro cargó para la clave")
    void valorDeShouldReturnValueOfRegistro() {
        Registro registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 4, null);
        BloqueInventario inventario = AvisoTestData.inventario(null, registro);

        assertThat(inventario.valorDe(registro, "quantity")).isEqualTo(4);
    }

    @Test
    @DisplayName("Devuelve null cuando el registro no cargó el dato")
    void valorDeShouldReturnNullWhenRegistroHasNoValue() {
        Registro registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 4, null);
        BloqueInventario inventario = AvisoTestData.inventario(null, registro);

        assertThat(inventario.valorDe(registro, "minimum")).isNull();
    }

    @Test
    @DisplayName("Devuelve null cuando la tabla no tiene un campo con esa clave")
    void valorDeShouldReturnNullWhenTablaHasNoCampoWithClave() {
        Registro registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 4, null);
        BloqueInventario inventario = AvisoTestData.inventario(null, registro);

        assertThat(inventario.valorDe(registro, "price")).isNull();
    }
}
