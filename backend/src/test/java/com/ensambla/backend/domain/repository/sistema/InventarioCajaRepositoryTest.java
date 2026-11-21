package com.ensambla.backend.domain.repository.sistema;

import com.ensambla.backend.domain.exception.ResourceNotFoundException;
import com.ensambla.backend.domain.model.bloque.Bloque;
import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.proyecto.ProyectoId;
import com.ensambla.backend.domain.model.sistema.Sistema;
import com.ensambla.backend.support.CajaTestData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InventarioCajaRepositoryTest {

    private final SistemaRepository sistemas = mock(SistemaRepository.class, CALLS_REAL_METHODS);

    private Bloque inventario;
    private Throwable rechazo;

    @Test
    @DisplayName("Obtiene únicamente el inventario del proyecto del negocio")
    void obtieneUnicamenteElInventarioDelProyectoDelNegocio() {
        dadoUnSistemaConSuInventario(CajaTestData.sistema());

        cuandoSePideElInventario(CajaTestData.PROYECTO, CajaTestData.INVENTARIO);

        entoncesSeDevuelveElInventario();
    }

    @Test
    @DisplayName("Oculta un proyecto que no pertenece al negocio")
    void ocultaUnProyectoQueNoPerteneceAlNegocio() {
        dadoUnSistemaConSuInventario(CajaTestData.sistema());

        cuandoSeIntentaPedirElInventario(ProyectoId.nuevo(), CajaTestData.INVENTARIO);

        entoncesSeRespondeQueNoSeEncontro();
    }

    @Test
    @DisplayName("Rechaza una tabla ausente")
    void rechazaUnaTablaAusente() {
        dadoUnSistemaConSuInventario(CajaTestData.sistema());

        cuandoSeIntentaPedirElInventario(CajaTestData.PROYECTO, BloqueId.nuevo());

        entoncesSeRespondeQueNoSeEncontro();
    }

    @Test
    @DisplayName("Una tabla de otro tipo no puede abrir una caja")
    void unaTablaDeOtroTipoNoPuedeAbrirUnaCaja() {
        dadoUnSistemaCuyaTablaEsDePedidos();

        cuandoSeIntentaPedirElInventario(CajaTestData.PROYECTO, CajaTestData.INVENTARIO);

        entoncesSeRespondeQueNoSeEncontro();
    }

    // --- dado ---

    private void dadoUnSistemaConSuInventario(Sistema sistema) {
        when(sistemas.findAllByNegocioId(CajaTestData.NEGOCIO)).thenReturn(List.of(sistema));
    }

    private void dadoUnSistemaCuyaTablaEsDePedidos() {
        Sistema sistema = CajaTestData.sistema();
        sistema.proyecto().getBloques().getFirst().setTipo(TipoBloque.PEDIDOS);
        dadoUnSistemaConSuInventario(sistema);
    }

    // --- cuando ---

    private void cuandoSePideElInventario(ProyectoId proyecto, BloqueId bloque) {
        inventario = sistemas.obtenerInventarioCaja(CajaTestData.NEGOCIO, proyecto, bloque);
    }

    private void cuandoSeIntentaPedirElInventario(ProyectoId proyecto, BloqueId bloque) {
        rechazo = catchThrowable(() -> cuandoSePideElInventario(proyecto, bloque));
    }

    // --- entonces ---

    private void entoncesSeDevuelveElInventario() {
        assertThat(inventario.getId()).isEqualTo(CajaTestData.INVENTARIO);
    }

    private void entoncesSeRespondeQueNoSeEncontro() {
        assertThat(rechazo).isInstanceOf(ResourceNotFoundException.class);
    }
}
