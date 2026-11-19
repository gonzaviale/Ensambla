package com.ensambla.backend.domain.useCase.sistema;

import com.ensambla.backend.domain.exception.DuplicateResourceException;
import com.ensambla.backend.domain.model.caja.ProteccionCaja;
import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GuardadoDeVentasDeCajaDesdeLaPantallaTest {

    private final Sistema guardado = CajaTestData.sistema();
    private Sistema propuesto;

    @Test
    @DisplayName("Guardar el sistema tal como lo leyó la pantalla no rechaza sus ventas de Caja")
    void guardarElSistemaTalComoLoLeyoLaPantallaNoRechazaSusVentasDeCaja() {
        dadoUnSistemaGuardadoConUnaVentaDeCaja();

        cuandoLaPantallaDevuelveElSistemaSinCambios();

        entoncesLasVentasDeCajaSeAceptan();
    }

    @Test
    @DisplayName("Cambiar el monto de una venta de Caja desde la pantalla sigue rechazándose")
    void cambiarElMontoDeUnaVentaDeCajaDesdeLaPantallaSigueRechazandose() {
        dadoUnSistemaGuardadoConUnaVentaDeCaja();

        cuandoLaPantallaDevuelveElSistemaConOtraCantidadVendida();

        entoncesLasVentasDeCajaSeRechazan();
    }

    // --- dado ---
    private void dadoUnSistemaGuardadoConUnaVentaDeCaja() {
        CajaTestData.agregarVentaDeCaja(guardado);
    }

    // --- cuando ---
    private void cuandoLaPantallaDevuelveElSistemaSinCambios() {
        propuesto = SistemaItem.de(guardado).aSistema(CajaTestData.NEGOCIO, guardado.id().toString(), CajaTestData.AHORA);
    }

    private void cuandoLaPantallaDevuelveElSistemaConOtraCantidadVendida() {
        cuandoLaPantallaDevuelveElSistemaSinCambios();
        Campo cantidad = columnaDeVentas("quantity");
        ventaDeCajaDe(propuesto).getValores().put(cantidad.getId().toString(), 99);
    }

    // --- entonces ---
    private void entoncesLasVentasDeCajaSeAceptan() {
        assertThatCode(this::validarLasVentasDeCaja).doesNotThrowAnyException();
    }

    private void entoncesLasVentasDeCajaSeRechazan() {
        assertThatThrownBy(this::validarLasVentasDeCaja).isInstanceOf(DuplicateResourceException.class);
    }

    private void validarLasVentasDeCaja() {
        ProteccionCaja.validar(List.of(guardado), propuesto, CajaTestData.AHORA);
    }

    private Campo columnaDeVentas(String clave) {
        return CajaTestData.ventasDe(propuesto).getCampos().stream()
                .filter(campo -> clave.equals(campo.getClave())).findFirst().orElseThrow();
    }

    private Registro ventaDeCajaDe(Sistema sistema) {
        return sistema.registrosPorBloque().get(CajaTestData.VENTAS).get(0);
    }
}
