package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/** Lo que Sami contesta en un turno del taller: qué decir, qué cambiar y, si hace falta, qué preguntar. */
public record RespuestaTaller(String texto, List<AccionTaller> acciones, OpcionesTaller opciones) {
}
