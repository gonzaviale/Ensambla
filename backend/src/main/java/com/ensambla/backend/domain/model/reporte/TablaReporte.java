package com.ensambla.backend.domain.model.reporte;

import com.ensambla.backend.domain.model.campo.Campo;
import com.ensambla.backend.domain.model.campo.TipoCampo;
import com.ensambla.backend.domain.model.registro.Registro;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Un reporte listo para descargar: una hoja con encabezados, filas y una fila final de totales. Cada valor es un
 * texto, un número ({@code BigDecimal}), una fecha ({@code LocalDate}), una fecha con hora ({@code LocalDateTime}) o
 * nulo.
 */
public record TablaReporte(String hoja, List<String> encabezados, List<List<Object>> filas, List<Object> totales) {

    static final String ENCABEZADO_CARGA = "Cargado";

    static final String FILA_ELIMINADA = "Fila eliminada";

    /**
     * Una columna por campo (en el orden dado) más la fecha de carga, y al final una fila de totales.
     * {@code etiquetas} da el nombre de las filas a las que apuntan las conexiones.
     */
    public static TablaReporte de(String nombreBloque, List<Campo> campos, ReporteRegistros reporte,
                                  Map<UUID, String> etiquetas) {
        List<String> encabezados = new ArrayList<>(campos.stream().map(Campo::getNombre).toList());
        encabezados.add(ENCABEZADO_CARGA);
        List<List<Object>> filas = reporte.filas().stream()
                .map(fila -> fila(fila, campos, etiquetas))
                .toList();
        return new TablaReporte(nombreBloque, encabezados, filas, totales(campos, reporte));
    }

    private static List<Object> fila(Registro fila, List<Campo> campos, Map<UUID, String> etiquetas) {
        List<Object> celdas = new ArrayList<>();
        for (Campo campo : campos) {
            celdas.add(celda(campo, fila.getValores().get(campo.getId().toString()), etiquetas));
        }
        celdas.add(fila.getCreadoEn().atZoneSameInstant(FiltroReporte.ZONA).toLocalDateTime());
        return celdas;
    }

    private static Object celda(Campo campo, Object valor, Map<UUID, String> etiquetas) {
        if (valor == null) {
            return null;
        }
        return switch (campo.getTipo()) {
            case NUMERO, MONEDA, FORMULA -> new BigDecimal(valor.toString());
            case FECHA -> LocalDate.parse(valor.toString());
            case SI_NO -> Boolean.TRUE.equals(valor) ? "Sí" : "No";
            case RELACION -> etiquetas.getOrDefault(UUID.fromString(valor.toString()), FILA_ELIMINADA);
            case TEXTO, OPCION, TEXTO_LARGO, EMAIL, TELEFONO, HORA -> valor.toString();
        };
    }

    /**
     * La suma de cada columna numérica. La etiqueta con la cantidad de filas va en la primera columna que no es
     * numérica o, si todas lo son, en la de la fecha de carga.
     */
    private static List<Object> totales(List<Campo> campos, ReporteRegistros reporte) {
        List<Object> totales = new ArrayList<>();
        int indiceEtiqueta = campos.size();
        for (int indice = 0; indice < campos.size(); indice++) {
            Campo campo = campos.get(indice);
            boolean numerico = campo.getTipo() == TipoCampo.NUMERO;
            totales.add(numerico ? reporte.totales().sumas().get(campo.getId()) : null);
            if (!numerico && indiceEtiqueta == campos.size()) {
                indiceEtiqueta = indice;
            }
        }
        totales.add(null);
        totales.set(indiceEtiqueta, etiquetaDeTotal(reporte.totales().cantidad()));
        return totales;
    }

    private static String etiquetaDeTotal(long cantidad) {
        return "Total (" + cantidad + (cantidad == 1 ? " fila)" : " filas)");
    }
}
