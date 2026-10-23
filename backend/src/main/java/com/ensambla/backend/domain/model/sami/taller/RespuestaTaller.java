package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que Sami contesta en un turno del taller: qué decir, qué cambiar y, si hace falta, qué preguntar.
 *
 * @param preguntas sus dudas del turno (hasta {@link CatalogoTaller#MAX_PREGUNTAS}); vacía si no preguntó nada
 */
public record RespuestaTaller(String texto, List<AccionTaller> acciones, List<OpcionesTaller> preguntas) {
}
