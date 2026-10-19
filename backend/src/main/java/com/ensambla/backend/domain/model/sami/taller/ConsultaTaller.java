package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/** Lo que se le manda a la IA en un turno: el mensaje, el estado compacto del sistema y la charla reciente. */
public record ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
}
