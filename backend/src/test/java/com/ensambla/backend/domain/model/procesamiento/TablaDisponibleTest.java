package com.ensambla.backend.domain.model.procesamiento;

import com.ensambla.backend.domain.model.bloque.BloqueId;
import com.ensambla.backend.domain.model.bloque.TipoBloque;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TablaDisponibleTest {

    private static final CampoConocido MONTO = new CampoConocido(CampoId.nuevo(), "Monto", TipoCampo.MONEDA, List.of());
    private static final CampoConocido NOMBRE = new CampoConocido(CampoId.nuevo(), "Nombre", TipoCampo.TEXTO, List.of());

    @Test
    @DisplayName("Las tablas de cosas (inventario, clientes, formularios) actualizan las filas existentes")
    void shouldUpdateExistingRowsInThingTables() {
        assertThat(tabla(TipoBloque.INVENTARIO).actualizaFilasExistentes()).isTrue();
        assertThat(tabla(TipoBloque.CLIENTES).actualizaFilasExistentes()).isTrue();
        assertThat(tabla(TipoBloque.FORMULARIO).actualizaFilasExistentes()).isTrue();
        assertThat(new TablaDisponible(tabla(TipoBloque.GASTOS).bloque(), "Panadería").actualizaFilasExistentes())
                .isTrue();
    }

    @Test
    @DisplayName("Las tablas de movimientos (ventas, gastos, pedidos) suman una fila por mensaje")
    void shouldAddRowsInMovementTables() {
        for (TipoBloque tipo : List.of(TipoBloque.VENTAS, TipoBloque.GASTOS, TipoBloque.PEDIDOS, TipoBloque.ENTRADAS,
                TipoBloque.SALIDAS, TipoBloque.RESUMEN, TipoBloque.NOTA)) {
            assertThat(tabla(tipo).actualizaFilasExistentes()).as(tipo.name()).isFalse();
        }
    }

    @Test
    @DisplayName("La columna que nombra a cada fila es la primera de texto, y puede no haberla")
    void identifyingFieldShouldBeTheFirstTextColumn() {
        assertThat(tabla(TipoBloque.INVENTARIO).campoIdentificador()).contains(NOMBRE);
        TablaDisponible sinTexto = new TablaDisponible(new BloqueLectura(BloqueId.nuevo(), "Solo montos",
                List.of(MONTO)), "Panadería", TipoBloque.INVENTARIO);
        assertThat(sinTexto.campoIdentificador()).isEmpty();
    }

    private static TablaDisponible tabla(TipoBloque tipo) {
        return new TablaDisponible(new BloqueLectura(BloqueId.nuevo(), "Tabla", List.of(MONTO, NOMBRE)),
                "Panadería", tipo);
    }
}
