package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que quedó de las herramientas que pidió el modelo después de validarlas.
 *
 * @param preguntas   las dudas válidas de la botonera, hasta {@link CatalogoTaller#MAX_PREGUNTAS}
 * @param descartadas cuántas llamadas se ignoraron por inválidas, desconocidas o pasarse del tope
 * @param registrosDescartados cuántas filas de {@code cargar_registros} no se pudieron cargar (sin el dato principal,
 *                             con datos que la pieza no tiene o pasadas del tope); Sami tiene que contarlo
 * @param piezas      cómo quedan las piezas después de aplicar las acciones (las sumadas en el turno, sin id)
 */
public record ValidacionTaller(List<AccionTaller> acciones, List<OpcionesTaller> preguntas, int descartadas,
                               int registrosDescartados, List<PiezaTaller> piezas) {
}
