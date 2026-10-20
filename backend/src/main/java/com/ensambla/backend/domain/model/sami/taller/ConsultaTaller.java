package com.ensambla.backend.domain.model.sami.taller;

import java.util.List;

/**
 * Lo que se le manda a la IA en un turno: el mensaje, el estado compacto del sistema, la charla reciente y, si se
 * reconoció el rubro, su kit.
 *
 * @param kit {@code null} si no se reconoció el rubro
 */
public record ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial, KitRubro kit) {

    public ConsultaTaller(String mensaje, EstadoTaller estado, List<MensajeTaller> historial) {
        this(mensaje, estado, historial, null);
    }
}
