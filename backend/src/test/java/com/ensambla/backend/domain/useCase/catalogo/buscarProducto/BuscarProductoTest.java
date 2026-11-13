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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BuscarProductoTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T12:00:00Z");
    private final CatalogoProductosPort catalogo = mock(CatalogoProductosPort.class);
    private final BuscarProducto caso = new BuscarProducto(catalogo, Clock.fixed(AHORA, ZoneOffset.UTC));

    @Test
    @DisplayName("Consulta la fuente y copia todos los datos públicos del producto")
    void executeShouldReturnProduct() {
        when(catalogo.buscar(FuenteCatalogo.SEPA, new CodigoBarras("77903754"), AHORA))
                .thenReturn(Optional.of(new ProductoCatalogo("77903754", "Alfajor", "Marca", "40 g", "foto")));

        var result = caso.execute(new BuscarProductoRequest("sepa", "77903754"));

        assertThat(result.producto().codigo()).isEqualTo("77903754");
        assertThat(result.producto().nombre()).isEqualTo("Alfajor");
        assertThat(result.producto().marca()).isEqualTo("Marca");
        assertThat(result.producto().presentacion()).isEqualTo("40 g");
        assertThat(result.producto().imagen()).isEqualTo("foto");
    }

    @Test
    @DisplayName("Una consulta sin coincidencia devuelve producto nulo")
    void executeShouldReturnMissingProduct() {
        when(catalogo.buscar(FuenteCatalogo.SEPA, new CodigoBarras("77903754"), AHORA)).thenReturn(Optional.empty());

        var result = caso.execute(new BuscarProductoRequest("sepa", "77903754"));

        assertThat(result.producto()).isNull();
    }

    @Test
    @DisplayName("La indisponibilidad del proveedor se propaga sin inventar una ausencia")
    void executeShouldPropagateUnavailableSource() {
        when(catalogo.buscar(FuenteCatalogo.SEPA, new CodigoBarras("77903754"), AHORA))
                .thenThrow(new CatalogoNoDisponibleException());

        assertThatThrownBy(() -> caso.execute(new BuscarProductoRequest("sepa", "77903754")))
                .isInstanceOf(CatalogoNoDisponibleException.class);
    }
}
