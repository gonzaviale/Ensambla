package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que quedó de las herramientas que pidió el modelo después de validarlas.
 *
 * @param descartadas cuántas llamadas se ignoraron por inválidas, desconocidas o pasarse del tope
 * @param piezas      cómo quedan las piezas después de aplicar las acciones (las sumadas en el turno, sin id)
 */
public record ValidacionTaller(List<AccionTaller> acciones, OpcionesTaller opciones, int descartadas,
                               List<PiezaTaller> piezas) {
}
