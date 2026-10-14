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

/** Escribe el reporte como planilla XLSX: encabezados en negrita, fechas con formato y la fila de totales al final. */
@Component
public class ExportadorReporteXlsxAdapter implements ExportadorReportePort {

    static final int MAX_CARACTERES_HOJA = 31;

    private static final String FORMATO_FECHA = "dd/mm/yyyy";

    private static final String FORMATO_FECHA_HORA = "dd/mm/yyyy hh:mm";

    @Override
    public byte[] exportar(TablaReporte tabla) {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribir(tabla, salida);
        return salida.toByteArray();
    }

    void escribir(TablaReporte tabla, OutputStream salida) {
        try {
            Workbook libro = new Workbook(salida, "Ensambla", "1.0");
            Worksheet hoja = libro.newWorksheet(nombreDeHoja(tabla.hoja()));
            for (int columna = 0; columna < tabla.encabezados().size(); columna++) {
                hoja.value(0, columna, tabla.encabezados().get(columna));
                hoja.style(0, columna).bold().set();
                hoja.width(columna, 18);
            }
            hoja.freezePane(0, 1);
            int fila = 1;
            for (List<Object> celdas : tabla.filas()) {
                escribirFila(hoja, fila++, celdas, false);
            }
            escribirFila(hoja, fila, tabla.totales(), true);
            libro.finish();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo escribir el reporte", e);
        }
    }

    private void escribirFila(Worksheet hoja, int fila, List<Object> celdas, boolean negrita) {
        for (int columna = 0; columna < celdas.size(); columna++) {
            escribirCelda(hoja, fila, columna, celdas.get(columna), negrita);
        }
    }

    private void escribirCelda(Worksheet hoja, int fila, int columna, Object valor, boolean negrita) {
        switch (valor) {
            case null -> { }
            case BigDecimal numero -> hoja.value(fila, columna, numero);
            case LocalDate fecha -> {
                hoja.value(fila, columna, fecha);
                hoja.style(fila, columna).format(FORMATO_FECHA).set();
            }
            case LocalDateTime fechaHora -> {
                hoja.value(fila, columna, fechaHora);
                hoja.style(fila, columna).format(FORMATO_FECHA_HORA).set();
            }
            default -> hoja.value(fila, columna, valor.toString());
        }
        if (negrita) {
            hoja.style(fila, columna).bold().set();
        }
    }

    /** Excel no admite más de 31 caracteres ni {@code : \ / ? * [ ]} en el nombre de una hoja. */
    static String nombreDeHoja(String nombre) {
        String limpio = nombre.replaceAll("[:\\\\/?*\\[\\]]", " ").strip();
        String corto = limpio.length() > MAX_CARACTERES_HOJA ? limpio.substring(0, MAX_CARACTERES_HOJA) : limpio;
        return corto.isEmpty() ? "Reporte" : corto;
    }
}
