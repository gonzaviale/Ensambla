package com.ensambla.backend.domain.model.aviso;

import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.support.AvisoTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BloqueInventarioTest {

    private static final RegistroId REGISTRO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");

    private Registro registro;
    private BloqueInventario inventario;
    private Object valor;

    @Test
    @DisplayName("Devuelve el valor que el registro cargó para la clave")
    void devuelveElValorQueElRegistroCargoParaLaClave() {
        dadoUnInventarioConUnRegistroDeYerbaConCuatroUnidades();

        cuandoSeConsultaElValorDe("quantity");

        entoncesElValorEs(4);
    }

    @Test
    @DisplayName("Devuelve null cuando el registro no cargó el dato")
    void devuelveNullCuandoElRegistroNoCargoElDato() {
        dadoUnInventarioConUnRegistroDeYerbaConCuatroUnidades();

        cuandoSeConsultaElValorDe("minimum");

        entoncesNoHayValor();
    }

    @Test
    @DisplayName("Devuelve null cuando la tabla no tiene un campo con esa clave")
    void devuelveNullCuandoLaTablaNoTieneUnCampoConEsaClave() {
        dadoUnInventarioConUnRegistroDeYerbaConCuatroUnidades();

        cuandoSeConsultaElValorDe("price");

        entoncesNoHayValor();
    }

    // --- dado ---

    private void dadoUnInventarioConUnRegistroDeYerbaConCuatroUnidades() {
        registro = AvisoTestData.registro(REGISTRO_ID.valor(), "Yerba", 4, null);
        inventario = AvisoTestData.inventario(null, registro);
    }

    // --- cuando ---

    private void cuandoSeConsultaElValorDe(String clave) {
        valor = inventario.valorDe(registro, clave);
    }

    // --- entonces ---

    private void entoncesElValorEs(Object esperado) {
        assertThat(valor).isEqualTo(esperado);
    }

    private void entoncesNoHayValor() {
        assertThat(valor).isNull();
    }
}
