package com.ensambla.backend.domain.useCase.reporte;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.CampoId;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;
import com.ensambla.backend.domain.model.registro.RegistroId;
import com.ensambla.backend.domain.model.registro.TotalesRegistros;
import com.ensambla.backend.domain.model.reporte.ReporteRegistros;
import com.ensambla.backend.domain.model.reporte.TablaReporte;
import com.ensambla.backend.domain.useCase.reporte.exportarReporte.ExportarReporteResponse;
import com.ensambla.backend.domain.useCase.reporte.generarReporte.GenerarReporteResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ReporteDtosTest {

    private static final CampoId NOMBRE_ID = CampoId.de("c0000000-0000-4000-8000-000000000001");
    private static final CampoId CANTIDAD_ID = CampoId.de("c0000000-0000-4000-8000-000000000002");
    private static final CampoId FECHA_ID = CampoId.de("c0000000-0000-4000-8000-000000000003");
    private static final CampoId PAGADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000004");
    private static final CampoId CLIENTE_ID = CampoId.de("c0000000-0000-4000-8000-000000000005");
    private static final CampoId TOTAL_ID = CampoId.de("c0000000-0000-4000-8000-000000000006");
    private static final CampoId ESTADO_ID = CampoId.de("c0000000-0000-4000-8000-000000000007");
    private static final RegistroId BETO_ID = RegistroId.de("d0000000-0000-4000-8000-000000000001");
    private static final RegistroId FILA_ID = RegistroId.de("d0000000-0000-4000-8000-000000000002");
    private static final OffsetDateTime CREADO = OffsetDateTime.parse("2026-10-13T01:30:00Z");

    @Test
    @DisplayName("La respuesta lleva las filas, los totales, si está truncado y el máximo que se muestra")
    void generarShouldMapRowsAndTotals() {
        Registro fila = Registro.builder().id(FILA_ID).valores(Map.of("k", "v")).creadoEn(CREADO).build();
        ReporteRegistros reporte = new ReporteRegistros(
                List.of(fila), new TotalesRegistros(7, Map.of(CANTIDAD_ID, new BigDecimal("12"))), true);

        GenerarReporteResponse result = GenerarReporteResponse.de(reporte, 50);

        assertThat(result.filas()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(FILA_ID));
        assertThat(result.totales().cantidad()).isEqualTo(7);
        assertThat(result.totales().sumas().get(CANTIDAD_ID)).isEqualByComparingTo("12");
        assertThat(result.truncado()).isTrue();
        assertThat(result.maximoFilas()).isEqualTo(50);
    }

    @Test
    @DisplayName("La tabla tiene una columna por campo y la fecha de carga, con cada valor del tipo de su columna")
    void deShouldTypeEachCellAfterItsColumn() {
        Registro fila = Registro.builder().id(FILA_ID).creadoEn(CREADO).valores(Map.of(
                NOMBRE_ID.toString(), "Pan",
                CANTIDAD_ID.toString(), 2.5,
                FECHA_ID.toString(), "2026-10-12",
                PAGADO_ID.toString(), true,
                CLIENTE_ID.toString(), BETO_ID.toString(),
                TOTAL_ID.toString(), new BigDecimal("5"),
                ESTADO_ID.toString(), "Entregado")).build();
        Registro vacia = Registro.builder().id(RegistroId.nuevo()).creadoEn(CREADO).valores(Map.of(
                PAGADO_ID.toString(), false,
                CLIENTE_ID.toString(), UUID.randomUUID().toString())).build();
        ReporteRegistros reporte = new ReporteRegistros(
                List.of(fila, vacia), new TotalesRegistros(2, Map.of(CANTIDAD_ID, new BigDecimal("2.5"))), false);

        TablaReporte result = TablaReporte.de(
                "Pedidos", givenCampos(), reporte, Map.of(BETO_ID, "Beto"));

        assertThat(result.hoja()).isEqualTo("Pedidos");
        assertThat(result.encabezados())
                .containsExactly("Nombre", "Cantidad", "Fecha", "Pagado", "Cliente", "Total", "Estado", "Cargado");
        assertThat(result.filas().get(0)).containsExactly(
                "Pan", new BigDecimal("2.5"), LocalDate.parse("2026-10-12"), "Sí", "Beto", new BigDecimal("5"),
                "Entregado", LocalDateTime.parse("2026-10-12T22:30:00"));
        assertThat(result.filas().get(1)).containsExactly(
                null, null, null, "No", "Fila eliminada", null, null, LocalDateTime.parse("2026-10-12T22:30:00"));
    }

    @Test
    @DisplayName("La fila de totales pone la etiqueta en la primera columna que no es numérica y la suma de las numéricas")
    void deShouldLabelTotalsInTheFirstNonNumericColumn() {
        ReporteRegistros reporte = new ReporteRegistros(
                List.of(), new TotalesRegistros(1, Map.of(CANTIDAD_ID, new BigDecimal("2.5"))), false);

        TablaReporte result = TablaReporte.de("Pedidos", givenCampos(), reporte, Map.of());

        assertThat(result.totales()).containsExactly(
                "Total (1 fila)", new BigDecimal("2.5"), null, null, null, null, null, null);
    }

    @Test
    @DisplayName("La etiqueta de totales va en la columna de la fecha de carga si todas las columnas son numéricas")
    void deShouldLabelTotalsInTheLoadDateColumnWhenEverythingIsNumeric() {
        List<Campo> soloNumeros = List.of(Campo.builder().id(CANTIDAD_ID).nombre("Cantidad")
                .tipo(TipoCampo.NUMERO).build());
        ReporteRegistros reporte = new ReporteRegistros(
                List.of(), new TotalesRegistros(3, Map.of(CANTIDAD_ID, BigDecimal.TEN)), false);

        TablaReporte result = TablaReporte.de("Pedidos", soloNumeros, reporte, Map.of());

        assertThat(result.totales()).containsExactly(BigDecimal.TEN, "Total (3 filas)");
    }

    @Test
    @DisplayName("El nombre del archivo es reporte, la tabla sin tildes ni espacios y la fecha")
    void deShouldBuildAFileNameFromTheTable() {
        byte[] contenido = {1, 2, 3};

        ExportarReporteResponse result = ExportarReporteResponse.de(
                contenido, "Órdenes de Compra", LocalDate.parse("2026-10-13"));

        assertThat(result.contenido()).isEqualTo(contenido);
        assertThat(result.nombreArchivo()).isEqualTo("reporte-ordenes-de-compra-20261013.xlsx");
        assertThat(ExportarReporteResponse.de(contenido, "¿¿??", LocalDate.parse("2026-01-02")).nombreArchivo())
                .isEqualTo("reporte-20260102.xlsx");
    }

    private static List<Campo> givenCampos() {
        return List.of(
                campo(NOMBRE_ID.valor(), "Nombre", TipoCampo.TEXTO),
                campo(CANTIDAD_ID.valor(), "Cantidad", TipoCampo.NUMERO),
                campo(FECHA_ID.valor(), "Fecha", TipoCampo.FECHA),
                campo(PAGADO_ID.valor(), "Pagado", TipoCampo.SI_NO),
                campo(CLIENTE_ID.valor(), "Cliente", TipoCampo.RELACION),
                campo(TOTAL_ID.valor(), "Total", TipoCampo.FORMULA),
                campo(ESTADO_ID.valor(), "Estado", TipoCampo.OPCION));
    }

    private static Campo campo(UUID id, String nombre, TipoCampo tipo) {
        return Campo.builder().id(CampoId.de(id)).nombre(nombre).tipo(tipo).build();
    }
}
