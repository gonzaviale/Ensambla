package com.ensambla.backend.domain.model.reporte;

import java.util.List;

/**
 * Un reporte listo para descargar: una hoja con encabezados, filas y una fila final de totales. Cada valor es un
 * texto, un número ({@code BigDecimal}), una fecha ({@code LocalDate}), una fecha con hora ({@code LocalDateTime}) o
 * nulo.
 */
public record TablaReporte(String hoja, List<String> encabezados, List<List<Object>> filas, List<Object> totales) {
}
