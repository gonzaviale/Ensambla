package com.ensambla.backend.infrastructure.reporte;

import com.ensambla.backend.domain.model.reporte.TablaReporte;
import com.ensambla.backend.domain.port.ExportadorReportePort;
import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** Escribe el reporte como planilla XLSX: encabezados en negrita, fechas con formato y la fila de totales al final. */
@Component
public class ExportadorReporteXlsxAdapter implements ExportadorReportePort {

    static final int MAX_CARACTERES_HOJA = 31;

    private static final String FORMATO_FECHA = "dd/mm/yyyy";

    private static final String FORMATO_FECHA_HORA = "dd/mm/yyyy hh:mm";

    private static final int ANCHO_DE_COLUMNA = 18;

    /** Cómo se escribe cada tipo de valor en la celda; lo que no está en la tabla va como texto. */
    private static final Map<Class<?>, EscritorDeCelda> ESCRITORES = Map.of(
            BigDecimal.class, (hoja, fila, columna, valor) -> hoja.value(fila, columna, (BigDecimal) valor),
            LocalDate.class, ExportadorReporteXlsxAdapter::escribirFecha,
            LocalDateTime.class, ExportadorReporteXlsxAdapter::escribirFechaHora);

    private static final EscritorDeCelda COMO_TEXTO =
            (hoja, fila, columna, valor) -> hoja.value(fila, columna, valor.toString());

    @FunctionalInterface
    private interface EscritorDeCelda {
        void escribir(Worksheet hoja, int fila, int columna, Object valor);
    }

    @Override
    public byte[] exportar(TablaReporte tabla) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(tabla, salida);
        return salida.toByteArray();
    }

    void escribir(TablaReporte tabla, OutputStream salida) {
        try {
            Workbook libro = new Workbook(salida, "Ensambla", "1.0");
            escribirHoja(libro.newWorksheet(nombreDeHoja(tabla.hoja())), tabla);
            libro.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el reporte", e);
        }
    }

    private void escribirHoja(Worksheet hoja, TablaReporte tabla) {
        escribirEncabezados(hoja, tabla.encabezados());
        hoja.freezePane(0, 1);
        int fila = 1;
        for (List<Object> celdas : tabla.filas()) {
            escribirFila(hoja, fila++, celdas, false);
        }
        escribirFila(hoja, fila, tabla.totales(), true);
    }

    private void escribirEncabezados(Worksheet hoja, List<String> encabezados) {
        for (int columna = 0; columna < encabezados.size(); columna++) {
            hoja.value(0, columna, encabezados.get(columna));
            hoja.style(0, columna).bold().set();
            hoja.width(columna, ANCHO_DE_COLUMNA);
        }
    }

    private void escribirFila(Worksheet hoja, int fila, List<Object> celdas, boolean negrita) {
        for (int columna = 0; columna < celdas.size(); columna++) {
            escribirCelda(hoja, fila, columna, celdas.get(columna), negrita);
        }
    }

    private void escribirCelda(Worksheet hoja, int fila, int columna, Object valor, boolean negrita) {
        escribirValor(hoja, fila, columna, valor);
        if (negrita) {
            hoja.style(fila, columna).bold().set();
        }
    }

    /** Una celda sin valor queda vacía. */
    private static void escribirValor(Worksheet hoja, int fila, int columna, Object valor) {
        if (valor == null) {
            return;
        }
        ESCRITORES.getOrDefault(valor.getClass(), COMO_TEXTO).escribir(hoja, fila, columna, valor);
    }

    private static void escribirFecha(Worksheet hoja, int fila, int columna, Object valor) {
        hoja.value(fila, columna, (LocalDate) valor);
        hoja.style(fila, columna).format(FORMATO_FECHA).set();
    }

    private static void escribirFechaHora(Worksheet hoja, int fila, int columna, Object valor) {
        hoja.value(fila, columna, (LocalDateTime) valor);
        hoja.style(fila, columna).format(FORMATO_FECHA_HORA).set();
    }

    /** Excel no admite más de 31 caracteres ni {@code : \ / ? * [ ]} en el nombre de una hoja. */
    static String nombreDeHoja(String nombre) {
        String limpio = nombre.replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
        String corto = limpio.length() > MAX_CARACTERES_HOJA ? limpio.substring(0, MAX_CARACTERES_HOJA) : limpio;
        return corto.isEmpty() ? "Reporte" : corto;
    }
}