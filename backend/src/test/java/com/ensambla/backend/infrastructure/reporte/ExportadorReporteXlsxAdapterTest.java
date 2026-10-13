package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.reporte.TablaReporte;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExportadorReporteXlsxAdapterTest {

    private final ExportadorReporteXlsxAdapter adapter = new ExportadorReporteXlsxAdapter();

    @Test
    @DisplayName("El archivo tiene los encabezados, cada fila con su tipo de dato y la fila de totales al final")
    void exportarShouldWriteHeadersTypedRowsAndTotals() throws IOException {
        TablaReporte tabla = new TablaReporte(
                "Pedidos",
                List.of("Producto", "Cantidad", "Fecha", "Pagado", "Cargado"),
                List.of(
                        Arrays.asList("Pan", new BigDecimal("2.5"), LocalDate.parse("2026-10-12"), "Sí",
                                LocalDateTime.parse("2026-10-12T22:30:00")),
                        Arrays.asList("Torta", new BigDecimal("1"), null, "No",
                                LocalDateTime.parse("2026-10-11T09:00:00"))),
                Arrays.asList("Total (2 filas)", new BigDecimal("3.5"), null, null, null));

        List<Row> filas = leer(adapter.exportar(tabla));

        assertThat(filas).hasSize(4);
        assertThat(filas.get(0).stream().map(Cell::getText)).containsExactly(
                "Producto", "Cantidad", "Fecha", "Pagado", "Cargado");
        assertThat(filas.get(1).getCellText(0)).isEqualTo("Pan");
        assertThat(filas.get(1).getCellAsNumber(1)).get().isEqualTo(new BigDecimal("2.5"));
        assertThat(filas.get(1).getCellAsDate(2)).get().isEqualTo(LocalDateTime.parse("2026-10-12T00:00:00"));
        assertThat(filas.get(1).getCellAsDate(4)).get().isEqualTo(LocalDateTime.parse("2026-10-12T22:30:00"));
        assertThat(filas.get(2).getCellText(2)).isEmpty();
        assertThat(filas.get(3).getCellText(0)).isEqualTo("Total (2 filas)");
        assertThat(filas.get(3).getCellAsNumber(1)).get().isEqualTo(new BigDecimal("3.5"));
    }

    @Test
    @DisplayName("Un valor de otro tipo se escribe como texto")
    void exportarShouldWriteOtherValuesAsText() throws IOException {
        TablaReporte tabla = new TablaReporte("Hoja", List.of("A"), List.of(List.of(true)), List.of());

        List<Row> filas = leer(adapter.exportar(tabla));

        assertThat(filas.get(1).getCellText(0)).isEqualTo("true");
    }

    @Test
    @DisplayName("La hoja tiene el nombre de la tabla sin los caracteres que Excel no admite")
    void nombreDeHojaShouldDropInvalidCharactersAndCapLength() {
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja("Pedidos: [2026]/?*")).isEqualTo("Pedidos   2026");
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja("x".repeat(40))).hasSize(31);
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja(" ??? ")).isEqualTo("Reporte");
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja("Corto")).isEqualTo("Corto");
    }

    @Test
    @DisplayName("Si no se puede escribir el archivo avisa con una excepción")
    void escribirShouldFailWhenTheStreamFails() {
        OutputStream roto = new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("sin espacio");
            }
        };
        TablaReporte tabla = new TablaReporte("Hoja", List.of("A"), List.of(List.of("x")), List.of());

        assertThatThrownBy(() -> adapter.escribir(tabla, roto)).isInstanceOf(UncheckedIOException.class);
    }

    private static List<Row> leer(byte[] contenido) throws IOException {
        try (ReadableWorkbook libro = new ReadableWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet hoja = libro.getFirstSheet();
            return hoja.read();
        }
    }
}
