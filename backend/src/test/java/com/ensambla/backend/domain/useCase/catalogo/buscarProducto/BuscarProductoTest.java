package com.ensambla.backend.domain.useCase.catalogo.buscarProducto;

import com.ensambla.backend.domain.exception.CatalogoNoDisponibleException;
import com.ensambla.backend.domain.model.catalogo.CodigoBarras;
import com.ensambla.backend.domain.model.catalogo.FuenteCatalogo;
import com.ensambla.backend.domain.model.catalogo.ProductoCatalogo;
import com.ensambla.backend.domain.port.CatalogoProductosPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarProductoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private static final CodigoBarras CODIGO = new CodigoBarras("77903754");

    private final CatalogoProductosPort catalogo = mock(CatalogoProductosPort.class);
    private final BuscarProducto buscarProducto = new BuscarProducto(catalogo, Clock.fixed(AHORA, ZoneOffset.UTC));

    private BuscarProductoResponse respuesta;
    private Throwable rechazo;

    @Test
    @DisplayName("Consulta la fuente y copia todos los datos públicos del producto")
    void consultaLaFuenteYCopiaTodosLosDatosPublicosDelProducto() {
        dadoQueLaFuenteTieneElAlfajor();

        cuandoSeBuscaElProductoEnSepa();

        entoncesSeCopianTodosLosDatosDelAlfajor();
    }

    @Test
    @DisplayName("Una consulta sin coincidencia devuelve producto nulo")
    void unaConsultaSinCoincidenciaDevuelveProductoNulo() {
        dadoQueLaFuenteNoTieneElProducto();

        cuandoSeBuscaElProductoEnSepa();

        entoncesNoHayProducto();
    }

    @Test
    @DisplayName("La indisponibilidad del proveedor se propaga sin inventar una ausencia")
    void laIndisponibilidadDelProveedorSePropagaSinInventarUnaAusencia() {
        dadoQueLaFuenteNoEstaDisponible();

        cuandoSeBuscaElProductoEnSepa();

        entoncesSePropagaQueElCatalogoNoEstaDisponible();
    }

    // --- dado ---

    private void dadoQueLaFuenteTieneElAlfajor() {
        ProductoCatalogo alfajor = new ProductoCatalogo("77903754", "Alfajor", "Marca", "40 g", "foto");
        when(catalogo.buscar(FuenteCatalogo.SEPA, CODIGO, AHORA)).thenReturn(Optional.of(alfajor));
    }

    private void dadoQueLaFuenteNoTieneElProducto() {
        when(catalogo.buscar(FuenteCatalogo.SEPA, CODIGO, AHORA)).thenReturn(Optional.empty());
    }

    private void dadoQueLaFuenteNoEstaDisponible() {
        when(catalogo.buscar(FuenteCatalogo.SEPA, CODIGO, AHORA)).thenThrow(new CatalogoNoDisponibleException());
    }

    // --- cuando ---

    private void cuandoSeBuscaElProductoEnSepa() {
        rechazo = catchThrowable(
                () -> respuesta = buscarProducto.execute(new BuscarProductoRequest("sepa", "77903754")));
    }

    // --- entonces ---

    private void entoncesSeCopianTodosLosDatosDelAlfajor() {
        assertThat(respuesta.producto().codigo()).isEqualTo("77903754");
        assertThat(respuesta.producto().nombre()).isEqualTo("Alfajor");
        assertThat(respuesta.producto().marca()).isEqualTo("Marca");
        assertThat(respuesta.producto().presentacion()).isEqualTo("40 g");
        assertThat(respuesta.producto().imagen()).isEqualTo("foto");
    }

    private void entoncesNoHayProducto() {
        assertThat(respuesta.producto()).isNull();
    }

    private void entoncesSePropagaQueElCatalogoNoEstaDisponible() {
        assertThat(rechazo).isInstanceOf(CatalogoNoDisponibleException.class);
    }
}
