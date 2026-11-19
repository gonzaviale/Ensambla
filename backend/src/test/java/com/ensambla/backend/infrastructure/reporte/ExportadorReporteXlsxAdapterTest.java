package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.reporte.TablaReporte;
import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.ReadableWorkbook;
import org.dhatim.fastexcel.reader.Row;
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
import static org.assertj.core.api.Assertions.catchThrowable;

class ExportadorReporteXlsxAdapterTest {

    private final ExportadorReporteXlsxAdapter adapter = new ExportadorReporteXlsxAdapter();
    private TablaReporte tabla;
    private List<Row> filas;
    private Throwable error;

    @Test
    @DisplayName("El archivo tiene los encabezados y una fila por cada fila de la tabla más la de totales")
    void elArchivoTieneLosEncabezadosLasFilasYLosTotales() throws IOException {
        dadoUnReporteDePedidos();

        cuandoSeExporta();

        entoncesElArchivoTieneFilas(4);
        entoncesLosEncabezadosSon("Producto", "Cantidad", "Fecha", "Pagado", "Cargado");
    }

    @Test
    @DisplayName("Cada celda se escribe con su tipo de dato: texto, número, fecha y fecha con hora")
    void cadaCeldaSeEscribeConSuTipoDeDato() throws IOException {
        dadoUnReporteDePedidos();

        cuandoSeExporta();

        entoncesLaCeldaDice(1, 0, "Pan");
        entoncesLaCeldaTieneElNumero(1, 1, new BigDecimal("2.5"));
        entoncesLaCeldaTieneLaFecha(1, 2, LocalDateTime.parse("2026-10-12T00:00:00"));
        entoncesLaCeldaTieneLaFecha(1, 4, LocalDateTime.parse("2026-10-12T22:30:00"));
    }

    @Test
    @DisplayName("Una celda sin valor queda vacía")
    void unaCeldaSinValorQuedaVacia() throws IOException {
        dadoUnReporteDePedidos();

        cuandoSeExporta();

        entoncesLaCeldaDice(2, 2, "");
    }

    @Test
    @DisplayName("La fila de totales va al final")
    void laFilaDeTotalesVaAlFinal() throws IOException {
        dadoUnReporteDePedidos();

        cuandoSeExporta();

        entoncesLaCeldaDice(3, 0, "Total (2 filas)");
        entoncesLaCeldaTieneElNumero(3, 1, new BigDecimal("3.5"));
    }

    @Test
    @DisplayName("Un valor de otro tipo se escribe como texto")
    void unValorDeOtroTipoSeEscribeComoTexto() throws IOException {
        dadoUnReporteConUnaCelda(true);

        cuandoSeExporta();

        entoncesLaCeldaDice(1, 0, "true");
    }

    @Test
    @DisplayName("La hoja se nombra sin los caracteres que Excel no admite")
    void laHojaSeNombraSinLosCaracteresQueExcelNoAdmite() {
        entoncesLaHojaDe("Pedidos: [2026]/?*", "Pedidos   2026");
    }

    @Test
    @DisplayName("El nombre de la hoja se corta en 31 caracteres")
    void elNombreDeLaHojaSeCortaEnTreintaYUnCaracteres() {
        entoncesElNombreDeLaHojaDeTieneCaracteres("x".repeat(40), 31);
    }

    @Test
    @DisplayName("Una hoja que queda sin nombre se llama Reporte")
    void unaHojaQueQuedaSinNombreSeLlamaReporte() {
        entoncesLaHojaDe(" ??? ", "Reporte");
    }

    @Test
    @DisplayName("Un nombre corto y válido queda igual")
    void unNombreCortoYValidoQuedaIgual() {
        entoncesLaHojaDe("Corto", "Corto");
    }

    @Test
    @DisplayName("Si no se puede escribir el archivo avisa con una excepción")
    void siNoSePuedeEscribirElArchivoAvisaConUnaExcepcion() {
        dadoUnReporteConUnaCelda("x");

        cuandoSeEscribeEnUnDiscoLleno();

        entoncesFallaLaEscritura();
    }

    private void dadoUnReporteDePedidos() {
        tabla = new TablaReporte(
                "Pedidos",
                List.of("Producto", "Cantidad", "Fecha", "Pagado", "Cargado"),
                List.of(
                        Arrays.asList("Pan", new BigDecimal("2.5"), LocalDate.parse("2026-10-12"), "Sí",
                                LocalDateTime.parse("2026-10-12T22:30:00")),
                        Arrays.asList("Torta", new BigDecimal("1"), null, "No",
                                LocalDateTime.parse("2026-10-11T09:00:00"))),
                Arrays.asList("Total (2 filas)", new BigDecimal("3.5"), null, null, null));
    }

    private void dadoUnReporteConUnaCelda(Object valor) {
        tabla = new TablaReporte("Hoja", List.of("A"), List.of(List.of(valor)), List.of());
    }

    private void cuandoSeExporta() throws IOException {
        try (ReadableWorkbook libro = new ReadableWorkbook(new ByteArrayInputStream(adapter.exportar(tabla)))) {
            filas = libro.getFirstSheet().read();
        }
    }

    private void cuandoSeEscribeEnUnDiscoLleno() {
        error = catchThrowable(() -> adapter.escribir(tabla, unDiscoLleno()));
    }

    private static OutputStream unDiscoLleno() {
        return new OutputStream() {
            @Override
            public void write(int b) throws IOException {
                throw new IOException("sin espacio");
            }
        };
    }

    private void entoncesElArchivoTieneFilas(int cantidad) {
        assertThat(filas).hasSize(cantidad);
    }

    private void entoncesLosEncabezadosSon(String... encabezados) {
        assertThat(filas.getFirst().stream().map(Cell::getText)).containsExactly(encabezados);
    }

    private void entoncesLaCeldaDice(int fila, int columna, String texto) {
        assertThat(filas.get(fila).getCellText(columna)).isEqualTo(texto);
    }

    private void entoncesLaCeldaTieneElNumero(int fila, int columna, BigDecimal numero) {
        assertThat(filas.get(fila).getCellAsNumber(columna)).contains(numero);
    }

    private void entoncesLaCeldaTieneLaFecha(int fila, int columna, LocalDateTime fecha) {
        assertThat(filas.get(fila).getCellAsDate(columna)).contains(fecha);
    }

    private void entoncesFallaLaEscritura() {
        assertThat(error).isInstanceOf(UncheckedIOException.class);
    }

    private static void entoncesLaHojaDe(String nombre, String seLlama) {
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja(nombre)).isEqualTo(seLlama);
    }

    private static void entoncesElNombreDeLaHojaDeTieneCaracteres(String nombre, int cantidad) {
        assertThat(ExportadorReporteXlsxAdapter.nombreDeHoja(nombre)).hasSize(cantidad);
    }
}