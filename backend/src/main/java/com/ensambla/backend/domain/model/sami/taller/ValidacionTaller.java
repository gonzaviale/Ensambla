package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que quedó de las herramientas que pidió el modelo después de validarlas.
 *
 * @param descartadas cuántas llamadas se ignoraron por inválidas, desconocidas o pasarse del tope
 */
public record ValidacionTaller(List<AccionTaller> acciones, OpcionesTaller opciones, int descartadas) {
}
