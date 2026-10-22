package com.ensambla.backend.domain.model.sami;

import java.util.List;
import java.util.UUID;

/**
 * Lo que Sami dedujo de un texto.
 *
 * @param plantillaId   id de la plantilla sugerida; {@code null} si ningún término coincidió
 * @param plantilla     nombre de la plantilla sugerida; {@code null} si ningún término coincidió
 * @param columnasExtra columnas que la plantilla no trae y el texto sugiere
 * @param confianza     de 0 a 1: cuánta evidencia hay y qué tan separada quedó la mejor de la segunda
 * @param puntajes      candidatas con puntos, de mayor a menor
 */
public record InterpretacionSami(
        UUID plantillaId,
        String plantilla,
        List<ColumnaSugerida> columnasExtra,
        double confianza,
        List<PuntajePlantilla> puntajes
) {

    public boolean alcanza(double umbral) {
        return plantilla != null && confianza >= umbral;
    }
}
